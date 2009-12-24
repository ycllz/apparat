/*
 * This file is part of Apparat.
 * 
 * Apparat is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Apparat is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Apparat. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright (C) 2009 Joa Ebert
 * http://www.joa-ebert.com/
 * 
 */
package apparat.abc

object AbcNameKind {
	val QName = 0x07
	val QNameA = 0x0d
	val RTQName = 0x0f
	val RTQNameA = 0x10
	val RTQNameL = 0x11
	val RTQNameLA = 0x12
	val Multiname = 0x09
	val MultinameA = 0x0e
	val MultinameL = 0x1b
	val MultinameLA = 0x1c
	val Typename = 0x1d
}

sealed abstract class AbcName(val kind: Int)
case class AbcQName(val name: Symbol, val namespace: AbcNamespace) extends AbcName(AbcNameKind.QName)
case class AbcQNameA(val name: Symbol, val namespace: AbcNamespace) extends AbcName(AbcNameKind.QNameA)
case class AbcRTQName(val name: Symbol) extends AbcName(AbcNameKind.RTQName)
case class AbcRTQNameA(val name: Symbol) extends AbcName(AbcNameKind.RTQNameA)
case object AbcRTQNameL extends AbcName(AbcNameKind.RTQNameL)
case object AbcRTQNameLA extends AbcName(AbcNameKind.RTQNameL)
case class AbcMultiname(val name: Symbol, val nsset: AbcNSSet) extends AbcName(AbcNameKind.Multiname)
case class AbcMultinameA(val name: Symbol, val nsset: AbcNSSet) extends AbcName(AbcNameKind.MultinameA)
case class AbcMultinameL(val nsset: AbcNSSet) extends AbcName(AbcNameKind.MultinameL)
case class AbcMultinameLA(val nsset: AbcNSSet) extends AbcName(AbcNameKind.MultinameLA)
case class AbcTypename(val name: AbcQName, val parameters: Array[AbcName]) extends AbcName(AbcNameKind.Typename) 