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
  (~/(?i)int/)                      : "long",
  (~/(?i)float|double|decimal|real/): "double",
  (~/(?i)datetime|timestamp/)       : "java.sql.Timestamp",
  (~/(?i)date/)                     : "java.sql.Date",
  (~/(?i)time/)                     : "java.sql.Time",
  (~/(?i)/)                         : "String"
]

FILES.chooseDirectoryAndSave("Choose directory", "Choose where to store generated files") { dir ->
  SELECTION.filter { it instanceof DasTable }.each { generate(it, dir) }
}

def generate(table, dir) {
  def className = javaName(table.getName(), true)
  def fields = calcFields(table)
  new File(dir, className + ".php").withPrintWriter { out -> generate(out, className, fields,table) }
}

def generate(out, className, fields, table) {
  //out.println "package $packageName"
  out.println ""
  out.println "/**"
  out.println " * @author = 郭磊"
  out.println " * @date = "+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())
  out.println " * @table = ${table.getName()}"
  out.println " */"
  out.println "class $className extends ModelBase"
  out.println "{"
  fields.each() {
    if (it.annos != "") out.println "  ${it.annos}"
    out.println "  /**"
    out.println "   * @field = ${it.cname}"
    out.println "   * @type = ${it.ctype}"
    out.println "   * @comment = ${it.comment.toString()}"
    out.println "   */"
    out.println "  protected \$${it.cname};"
    out.println ""
  }
  out.println ""
  fields.each() {
    out.println ""
    out.println "  /**"
    out.println "   * @method = getter"
    out.println "   * @field = ${it.cname}"
    out.println "   * @comment = ${it.comment.toString()}"
    out.println "   */"
    out.println "  public function get${it.name.capitalize()}()"
    out.println "  {"
    out.println "    return \$this->\$${it.cname};"
    out.println "  }"
    out.println ""
    out.println "  /**"
    out.println "   * @method = setter"
    out.println "   * @field = ${it.cname}"
    out.println "   * @comment = ${it.comment.toString()}"
    out.println "   */"
    out.println "  public function set${it.name.capitalize()}(\$${it.cname})"
    out.println "  {"
    out.println "    \$this->${it.cname} = \$${it.cname};"
    out.println "    return \$this;"
    out.println "  }"
    out.println ""
  }

  out.println ""
  out.println "  /**"
  out.println "   * @method = initialize"
  out.println "   */"
  out.println "  public function initialize()"
  out.println "  {"
  out.println "    \$this->setSource(\"${table.getName()}\");"
  out.println "    parent::initialize();"
  out.println "  }"
  out.println ""
  out.println "  /**"
  out.println "   * @method = getSource"
  out.println "   */"
  out.println "  public function getSource()"
  out.println "  {"
  out.println "    return \"${table.getName()}\";"
  out.println "  }"
  out.println ""
  out.println "  /**"
  out.println "   * @method = find"
  out.println "   */"
  out.println "  public static function find(\$parameters = null)"
  out.println "  {"
  out.println "    return parent::find(\$parameters);"
  out.println "  }"
  out.println ""
  out.println "  /**"
  out.println "   * @method = findFirst"
  out.println "   */"
  out.println "  public static function findFirst(\$parameters = null)"
  out.println "  {"
  out.println "    return parent::findFirst(\$parameters);"
  out.println "  }"
  out.println ""
  out.println "  /**"
  out.println "   * @method = columnMap"
  out.println "   */"
  out.println "  public function columnMap()"
  out.println "  {"
  out.println "    return ["
  fields.each() {
    out.println "        \"${it.cname}\" => \"${it.cname}\","
  }
  out.println "    ];"
  out.println "  }"

  out.println "}"
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
