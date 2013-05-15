/*
 * This file is part of Apparat.
 *
 * Copyright (C) 2010 Joa Ebert
 * http://www.joa-ebert.com/
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package apparat.tools.tdsi

import java.io.{File => JFile}
import apparat.utils.TagContainer
import apparat.abc._
import analysis.QuickAbcConstantPoolBuilder
import apparat.swf._
import annotation.tailrec
import apparat.bytecode.optimization._
import apparat.tools.{ApparatConfiguration, ApparatApplication, ApparatTool}

/**
 * @author Joa Ebert
 */
object TurboDieselSportInjection {
  def main(args: Array[String]): Unit = ApparatApplication(new TDSITool, args)

  class TDSITool extends ApparatTool {
    var input: JFile = _
    var output: JFile = _
    var alchemy = true
    var macros = true
    var inline = true
    var fixAlchemy = false
    var asm = true
    var libraries = List.empty[JFile]

    override def name = "Turbo Diesel Sport Injection"

    override def help = """  -i [file]			Input file
  -o [file]			Output file (optional)
  -f (true|false)   Fix bytecode generated by Alchemy
  -a (true|false)	Inline Alchemy operations
  -e (true|false)	Inline expansion
  -m (true|false)	Macro expansion
  -s (true|false)   Asm expansion
  -l [file0""" + JFile.pathSeparatorChar + "file1" + JFile.pathSeparatorChar + "..." + JFile.pathSeparatorChar + "fileN] External libraries"

    override def configure(config: ApparatConfiguration): Unit = configure(TDSIConfigurationFactory fromConfiguration config)

    def configure(config: TDSIConfiguration): Unit = {
      input = config.input
      output = config.output
      alchemy = config.alchemyExpansion
      macros = config.macroExpansion
      inline = config.inlineExpansion
      fixAlchemy = config.fixAlchemy
      asm = config.asmExpansion
      libraries = config.externalLibraries
    }

    // clear some apparat internal classes from the swf
    // TODO more to add
    val classesToRemove = List(AbcQName('Structure, AbcNamespace(22, Symbol("apparat.memory"))))
    val methodsToRemove = List(AbcQName('map, AbcNamespace(22, Symbol("apparat.memory"))))

    def cleanABC(abc:Abc) {
      var newScripts = List.empty[AbcScript]
      var newMethods = abc.methods
      var newMetadatas = abc.metadata
      for {
        script <- abc.scripts
      } {
        var checkForEmptyScript = false
        var checkForInit = false
        var newTrait = List.empty[AbcTrait]
        for {
          trt <- script.traits
        } {
          trt match {
            case AbcTraitClass(aName, _, nt, md) if (classesToRemove.exists(_ == aName) || (nt.inst.base.exists(n => classesToRemove.exists(_ == n)))) =>
              checkForEmptyScript = true
              md match {
                case Some(metas) => newMetadatas = newMetadatas.filter(m => metas.exists(_ != m))
                case _ =>
              }
            case AbcTraitMethod(aName, _, m, _, _, _) if (methodsToRemove.exists(_ == aName)) =>
              checkForEmptyScript = true
              checkForInit = true
              newMethods = newMethods.filter(_ != m)
            case t@_ => newTrait = t :: newTrait
          }
        }
        if (checkForEmptyScript) {
          if (newTrait.size > 0) {
            script.traits = newTrait.reverse.toArray
            newScripts = script :: newScripts
          } else {
            newMethods = newMethods.filter(_ != script.init)
          }
        } else {
          newScripts = script :: newScripts
        }
      }
      if (abc.scripts.size != newScripts.size) {
        abc.scripts = newScripts.reverse.toArray
      }
      abc.methods = newMethods
      abc.metadata= newMetadatas
    }


    override def run() = {
      SwfTags.tagFactory = (kind: Int) => kind match {
        case SwfTags.DoABC => Some(new DoABC)
        case SwfTags.DoABC1 => Some(new DoABC)
        case _ => None
      }

      val abcLibraries = libraries flatMap {
        library => {
          (TagContainer fromFile library).tags collect {
            case x: DoABC => x
          } map {
            Abc fromDoABC _
          }
        }
      }

      abcLibraries foreach {
        _.loadBytecode()
      }

      val source = input
      val target = output
      val cont = TagContainer fromFile source
      val allABC = (for (doABC <- cont.tags collect {
        case doABC: DoABC => doABC
      }) yield (doABC -> (Abc fromDoABC doABC))).toMap
      val environment = allABC.valuesIterator.toList ::: abcLibraries
      val macroExpansion = if (macros) Some(new MacroExpansion(environment)) else None
      val inlineExpansion = if (inline) Some(new InlineExpansion(environment)) else None
      val memoryExpansion = if (alchemy) Some(new MemoryHelperExpansion(environment)) else None

      allABC foreach {
        _._2.loadBytecode()
      }

      var rebuildCpoolSet = Set.empty[Abc]

      if (asm) {
        for ((doABC, abc) <- allABC) {
          for {
            method <- abc.methods
            body <- method.body
            bytecode <- body.bytecode
          } {
            var rebuildCpool = false

            @tailrec def modifyBytecode(counter: Int): Unit = {
              var modified = false

              if (AsmExpansion(bytecode)) {
                modified = true
                rebuildCpool = true
              }

              if (modified && (counter > 0)) {
                modifyBytecode(counter - 1)
              } else if (counter <= 0) {
                log.warning("Too many optimisation for " + method.name)
              }
            }

            PeepholeOptimizations(bytecode)
            modifyBytecode(31)

            if (rebuildCpool) {
              rebuildCpoolSet += abc
            }
          }
        }
      }

      for ((doABC, abc) <- allABC) {
        var rebuildCpool = rebuildCpoolSet.contains(abc)

        for {
          method <- abc.methods
          body <- method.body
          bytecode <- body.bytecode
        } {
          @tailrec def modifyBytecode(counter: Int): Unit = {
            var modified = false

            if (inline && (inlineExpansion.get.expand(bytecode))) {
              modified = true
              rebuildCpool = true
            }

            if (macros && (macroExpansion.get.expand(bytecode))) {
              modified = true
              rebuildCpool = true
            }

            if (alchemy) {
              modified |= InlineMemory(bytecode)
              // don't run memory expansion within Macro
              if (! {
                macroExpansion match {
                  case Some(me) => {
                    abc.types.exists(n => (n.inst.base.getOrElse(AbcConstantPool.EMPTY_NAME) == me.apparatMacro) && (n.klass.traits.exists(p => p match {
                      case AbcTraitMethod(_, _, meth, _, _, _) if (meth == method) => true
                      case _ => false
                    })))
                  }
                  case _ => false
                }
              }) {
                modified |= memoryExpansion.get expand bytecode
              }
            }

            if (fixAlchemy) {
              modified |= AlchemyOptimizations(bytecode)
            }

            modified |= PeepholeOptimizations(bytecode)

            if (modified && (counter > 0)) {
              modifyBytecode(counter - 1)
            } else if (counter <= 0) {
              log.warning("Too many optimisation for " + method.name)
            }
          }

          modifyBytecode(31)
        }

        cleanABC(abc)

        if (rebuildCpool) {
          //
          // We have to rebuild the cpool here since both Macro and Inline
          // expansion could include operations from a different ABC
          // and in that case its values do not belong to the cpool.
          //

          log.info("Cpool rebuild required.")
          abc.cpool = QuickAbcConstantPoolBuilder using abc
        }

        abc.saveBytecode()
        abc write doABC
      }

      cont write target
    }
  }

}
