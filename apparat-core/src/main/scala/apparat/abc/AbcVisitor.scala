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
package apparat.abc

trait AbcVisitor {
	def visit(value: Abc): Unit = {}
	def visit(value: AbcClass): Unit = {}
	def visit(value: AbcConstantPool): Unit = {}
	def visit(value: AbcExceptionHandler): Unit = {}
	def visit(value: AbcInstance): Unit = {}
	def visit(value: AbcMetadata): Unit = {}
	def visit(value: AbcMethod): Unit = {}
	def visit(value: AbcMethodBody): Unit = {}
	def visit(value: AbcMethodParameter): Unit = {}
	def visit(value: AbcNominalType): Unit = {}
	def visit(value: AbcScript): Unit = {}
	def visit(value: AbcTrait): Unit = {}
}
