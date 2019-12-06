import com.intellij.database.model.DasTable
import com.intellij.database.util.Case
import com.intellij.database.util.DasUtil
import java.io.*
import java.text.SimpleDateFormat


/*
 * Available context bindings:
 *   SELECTION   Iterable<DasObject>
 *   PROJECT     project
 *   FILES       files helper
 */

packageName = "com.sample;"
typeMapping = [
  (~/(?i)int/)                      : "Integer",
  (~/(?i)float|double|decimal|real/): "Float",
  (~/(?i)datetime|timestamp/)       : "DateTime",
  (~/(?i)date/)                     : "Date",
  (~/(?i)time/)                     : "Time",
  (~/(?i)/)                         : "String"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
  SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

def generate(table, dir) {
  def className = javaName(table.getName(), true)
  def fields = calcFields(table)

  new File(dir, className + ".py").withPrintWriter { out -> generate(out, className, fields,table) }
}

def generate(out, className, fields, table) {
  //out.println "package $packageName"
  out.println "#!/usr/bin/env python3"
  out.println "# -*- coding: UTF-8 -*-"
  out.println ""
  out.println ""

  out.println "class $className(ModelBase):"
  out.println "\t\"\"\""
  out.println "\tSQLAlchemy Model"
  out.println "\tcreate by auto generate"
  out.println "\t@author = 郭磊"
  out.println "\t@email = 174000902@qq.com"
  out.println "\t@phone = 15210720528"
  out.println "\t@date = "+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
  out.println "\t@table = ${table.getName()}"
  out.println "\t\"\"\""
  out.println ""
  out.println "\t\"\"\""
  out.println "\t@table = ${table.getName()}"
  out.println "\t\"\"\""
  out.println "\t__tablename__ = \"${table.getName()}\""
  out.println ""
  fields.each() {
    if (it.annos != "") out.println "  ${it.annos}"
    out.println "\t\"\"\""
    out.println "\tfield = ${it.cname}"
    out.println "\ttype = ${it.ctype}"
    out.println "\tcomment = ${it.comment.toString()}"
    out.println "\t\"\"\""
    out.println "\t${it.cname} = Column(\"${it.cname}\", ${it.type})"
    out.println ""
  }
  out.println ""
  out.println "\tdef to_dict(self):"
  out.println "\t\t\"\"\""
  out.println "\t\tmethod to dict"
  out.println "\t\t:return dict"
  out.println "\t\t\"\"\""
  out.println "\t\tdict_obj = self.__dict__"
  out.println "\t\tif \"_sa_instance_state\" in dict_obj:"
  out.println "\t\t\tdict_obj.pop(\"_sa_instance_state\")"
  out.println "\t\treturn dict_obj"

}

def calcFields(table) {
  DasUtil.getColumns(table).reduce([]) { fields, col ->
    def spec = Case.LOWER.apply(col.getDataType().getSpecification())
    def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
    fields += [[
                 name : javaName(col.getName(), false),
                 cname: col.getName(),
                 ctype: col.getDataType(),
                 type : typeStr,
                 comment: col.getComment(),
                 annos: "",
                 key: col.getProperties()]]
  }
}

def javaName(str, capitalize) {
  def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
    .collect { Case.LOWER.apply(it).capitalize() }
    .join("")
    .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
  capitalize || s.length() == 1? s : Case.LOWER.apply(s[0]) + s[1..-1]
}
