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
  (~/(?i)int/)                      : "int64",
  (~/(?i)float|double|decimal|real/): "float64",
  (~/(?i)datetime|timestamp/)       : "time.Time",
  (~/(?i)date/)                     : "time.Time",
  (~/(?i)time/)                     : "time.Time",
  (~/(?i)/)                         : "string"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
  SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

def generate(table, dir) {
  def className = javaName(table.getName(), true)
  def fields = calcFields(table)
  new File(dir, className + ".go").withPrintWriter { out -> generate(out, className, fields,table) }
}

def generate(out, className, fields,table) {
  //out.println "package $packageName"
  out.println ""
  out.println " // @author = 郭磊"
  out.println " // @date = "+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
  out.println " // @table = ${table.getName()}"
  out.println "type $className struct{"
  out.println ""
  fields.each() {
    if (it.annos != "") out.println "  ${it.annos}"
    out.println "  ${it.name.capitalize()} ${it.type} `orm:\"column(${it.cname});\"` // ${it.comment.toString()}"
  }
  out.println ""
  out.println "}"
  out.println ""
  out.println "// setter table name"
  out.println "func (this *$className) TableName() string {"
  out.println "  return \"${table.getName()}\""
  out.println "}"
  out.println ""
  out.println "// find first by custom columns"
  out.println "func (this *$className) FindFirstByColumns(columns ...string) (*$className, error) {"
  out.println "  ormer := GetOrmer()"
  out.println "  ormer.Using(GetAliasName(\"slaver\"))"
  out.println "  err := ormer.Read(this, columns...)"
  out.println "  return this, err"
  out.println "}"
  out.println ""
  out.println "// Insert to Database"
  out.println "func (this *$className) Insert() (int64, error) {"
  out.println "  this.CreateTime = time.Now().Unix()"
  out.println "  ormer := GetOrmer()"
  out.println "  ormer.Using(GetAliasName(\"master\"))"
  out.println "  return ormer.Insert(this)"
  out.println "}"
  out.println ""
  out.println "// Update All Columns to Database"
  out.println "func (this *$className) Modify() (int64, error) {"
  out.println "  this.LastModifyTime = time.Now().Unix()"
  out.println "  ormer := GetOrmer()"
  out.println "  ormer.Using(GetAliasName(\"master\"))"
  out.println "  return ormer.Update(this)"
  out.println "}"
  out.println "// Update Custom Columns to Database"
  out.println "func (this *$className) ModifyOfColumns(columns ...string) (int64, error) {"
  out.println "  this.LastModifyTime = time.Now().Unix()"
  out.println "  columns = append(columns, \"LastModifyTime\", \"LastModifyAcUserId\")"
  out.println "  ormer := GetOrmer()"
  out.println "  ormer.Using(GetAliasName(\"master\"))"
  out.println "  return ormer.Update(this, columns...)"
  out.println "}"

  out.println ""
  out.println "// Soft Delete to Database"
  out.println "func (this *$className) Remove() (int64, error) {"
  out.println "  this.RemoveFlag = 1"
  out.println "  this.RemoveTime = time.Now().Unix()"
  out.println "  ormer := GetOrmer()""RemoveFlag"
  out.println "  ormer.Using(GetAliasName(\"master\"))"
  out.println "  return ormer.Update(this, \"RemoveFlag\", \"RemoveTime\", \"RemoveAcUserId\")"
  out.println "}"
  out.println ""
  out.println "// Delete to Database"
  out.println "func (this *$className) Delete() (int64, error) {"
  out.println "  ormer := GetOrmer()"
  out.println "  ormer.Using(GetAliasName(\"master\"))"
  out.println "  return ormer.Delete(this)"
  out.println "}"
  out.println ""
}

def calcFields(table) {
  DasUtil.getColumns(table).reduce([]) { fields, col ->
    def spec = Case.LOWER.apply(col.getDataType().getSpecification())
    def typeStr = typeMapping.find { p, t -> p.matcher(spec).find() }.value
    fields += [[
                 name : javaName(col.getName(), false),
                 type : typeStr,
                 name : javaName(col.getName(), false),
                 cname: col.getName(),
                 ctype: col.getDataType(),
                 type : typeStr,
                 comment: col.getComment(),
                 defaultValue: col.getDefault(),
                 primary: DasUtil.isPrimary(col),
                 annos: ""]]
  }
}

def javaName(str, capitalize) {
  def s = com.intellij.psi.codeStyle.NameUtil.splitNameIntoWords(str)
    .collect { Case.LOWER.apply(it).capitalize() }
    .join("")
    .replaceAll(/[^\p{javaJavaIdentifierPart}[_]]/, "_")
  capitalize || s.length() == 1? s : Case.LOWER.apply(s[0]) + s[1..-1]
}
