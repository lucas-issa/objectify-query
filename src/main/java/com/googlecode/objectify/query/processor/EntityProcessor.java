package com.googlecode.objectify.query.processor;

import static javax.lang.model.SourceVersion.RELEASE_6;

import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.util.ElementFilter;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Indexed;
import com.googlecode.objectify.annotation.NotSaved;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Unindexed;
import com.googlecode.objectify.query.annotation.List;
import com.googlecode.objectify.query.annotation.List.KeyType;

@SupportedAnnotationTypes("com.googlecode.objectify.annotation.Entity")
@SupportedSourceVersion(RELEASE_6)
public class EntityProcessor extends AbstractProcessor {

  private ProcessingEnvironment env;

  @Override
  public synchronized void init(ProcessingEnvironment env) {
    super.init(env);

    this.env = env;
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations,
      RoundEnvironment roundEnv) {

    if (!roundEnv.processingOver()) {

      for (TypeElement currAnnotation : annotations) {

        if (currAnnotation.getQualifiedName().contentEquals(
            Entity.class.getName())) {

          for (Element entity : roundEnv.getElementsAnnotatedWith(currAnnotation)) {
            this.processEntity(entity);
          }

        }
      }

    }

    return true;
  }

  private void processEntity(Element entityElement) {
    PrintWriter out = null;

    String entityPackageName = env.getElementUtils().getPackageOf(entityElement).getQualifiedName().toString();
    String entityName = entityElement.getSimpleName().toString();
    String queryName = entityName + "Query";
    String queryPackageName = entityPackageName.replaceAll("\\.shared\\.",
        ".server.");
    String queryQName = queryPackageName + "." + queryName;

    Unindexed unindexedClass = entityElement.getAnnotation(Unindexed.class);

    VariableElement parentField = null;

    try {
      out = new PrintWriter(
          new BufferedWriter(this.env.getFiler().createSourceFile(queryQName,
              entityElement).openWriter()));

      out.println("package " + queryPackageName + ";");
      out.println();
      out.println("import java.util.ArrayList;");
      out.println("import java.util.List;");
      out.println("import java.util.Map;");
      out.println("import java.util.Set;");
      out.println();
      out.println("import com.google.appengine.api.datastore.Cursor;");
      out.println("import com.google.appengine.api.datastore.QueryResultIterable;");
      out.println("import com.google.appengine.api.datastore.QueryResultIterator;");
      out.println("import com.googlecode.objectify.Key;");
      out.println("import com.googlecode.objectify.Objectify;");
      out.println("import com.googlecode.objectify.ObjectifyOpts;");
      out.println("import com.googlecode.objectify.ObjectifyService;");

      out.println("import com.googlecode.objectify.Query;");
      out.println("import com.googlecode.objectify.query.shared.ListPage;");
      out.println("import " + entityPackageName + "." + entityName + ";");
      out.println();
      out.println("public class " + queryName + " implements Query<"
          + entityName + "> { ");
      out.println();
      out.println("private final Query<" + entityName + "> query;");
      out.println("private Objectify lazyOfy;");
      out.println();
      out.println("  public " + queryName + "(Query<" + entityName
          + "> query) {");
      out.println("    this.query = query;");
      out.println("  }");
      out.println();

      for (VariableElement fieldElement : ElementFilter.fieldsIn(env.getElementUtils().getAllMembers(
          (TypeElement) entityElement))) {

        Unindexed unindexedField = fieldElement.getAnnotation(Unindexed.class);
        // Transient transientField =
        // fieldElement.getAnnotation(Transient.class);
        NotSaved notSavedField = fieldElement.getAnnotation(NotSaved.class);
        Indexed indexedField = fieldElement.getAnnotation(Indexed.class);

        Parent parent = fieldElement.getAnnotation(Parent.class);
        if (parentField == null && parent != null) {
          parentField = fieldElement;
        }

        if (unindexedField != null && unindexedField.value().length == 0) {
          // @Unindexed field without If... parameter
        } else if (notSavedField != null) {
          // @NotSaved field
          // } else if(transientField != null) {
          // // @Transient field
        } else if (unindexedClass != null && indexedField == null) {
          // @Unindexed class and field is not @Indexed
        } else {          
          String fieldName = fieldElement.getSimpleName().toString();                              
          String fieldType = env.getTypeUtils().asMemberOf((DeclaredType)entityElement.asType(), fieldElement).toString();
          
          out.println("  public " + queryName + " filterBy"
              + fieldName.substring(0, 1).toUpperCase()
              + fieldName.substring(1) + "(" + fieldType + " " + fieldName
              + ") {");
          if (parent != null) {
            out.println("    this.query.ancestor(" + fieldName + ");");
            out.println("    return this;");
          } else {
            out.println("    this.query.filter(\"" + fieldName + "\", "
                + fieldName + ");");
            out.println("    return this;");
          }
          out.println("  }");
          out.println();

          if (parent == null) {
            out.println("  public " + queryName + " filterBy"
                + fieldName.substring(0, 1).toUpperCase()
                + fieldName.substring(1) + "(String operation, Object value) {");
            out.println("    this.query.filter(\"" + fieldName
                + " \" + operation, value);");
            out.println("    return this;");
            out.println("  }");
            out.println();
          }
        }
      }

      out.println("  @Override");
      out.println("  public " + queryName + " ancestor(Object keyOrEntity) {");
      out.println("    this.query.ancestor(keyOrEntity);");
      out.println("    return this;");
      out.println("  }");
      out.println();
      out.println("  @Deprecated");
      out.println("  @Override");
      out.println("  public int countAll() {");
      out.println("    return this.query.countAll();");
      out.println("  }");
      out.println();
      out.println("  @Deprecated");
      out.println("  @Override");
      out.println("  public " + queryName + " cursor(Cursor value) {");
      out.println("    this.query.cursor(value);");
      out.println("    return this;");
      out.println("  }");
      out.println();
      out.println("  @Override");
      out.println("  public int count() {");
      out.println("    return this.query.count();");
      out.println("  }");
      out.println();
      out.println("  @Override");
      out.println("  public Query<" + entityName + "> endCursor(Cursor value) {");
      out.println("    return this.query.endCursor(value);");
      out.println("  }");
      out.println();
      out.println("  @Override");
      out.println("  public Query<" + entityName + "> startCursor(Cursor value) {");
      out.println("    return this.query.startCursor(value);");
      out.println("  }");
      out.println();
      out.println("  @Override");
      out.println("  public Query<" + entityName + "> clone() {");
      out.println("    return this.query.clone();");
      out.println("  }");
      out.println();
      out.println("  @Deprecated");
      out.println("  @Override");
      out.println("  public QueryResultIterable<" + entityName + "> fetch() {");
      out.println("    return this.query.fetch();");
      out.println("  }");
      out.println();
      out.println("  @Override");
      out.println("  public QueryResultIterable<Key<" + entityName
          + ">> fetchKeys() {");
      out.println("    return this.query.fetchKeys();");
      out.println("  }");
      out.println();
      out.println("  @Override");
      out.println("  public <V> Set<Key<V>> fetchParentKeys() {");
      out.println("    return this.query.fetchParentKeys();");
      out.println("  }");
      out.println();
      out.println("  @Override");
      out.println("  public <V> Map<Key<V>, V> fetchParents() {");
      out.println("    return this.query.fetchParents();");
      out.println("  }");
      out.println();
      out.println("  @Override");
      out.println("  public Query<" + entityName
          + "> filter(String condition, Object value) {");
      out.println("    return this.query.filter(condition, value);");
      out.println("  }");
      out.println();
      out.println("  @Override");
      out.println("  public " + entityName + " get() {");
      out.println("    return this.query.get();");
      out.println("  }");
      out.println();
      out.println("  @Override");
      out.println("  public Key<" + entityName + "> getKey() {");
      out.println("    return this.query.getKey();");
      out.println("  }");
      out.println();
      out.println("  @Override");
      out.println("  public Query<" + entityName + "> limit(int value) {");
      out.println("    return new " + queryName + "(this.query.limit(value));");
      out.println("  }");
      out.println();
      out.println("  @Override");
      out.println("  public List<" + entityName + "> list() {");
      out.println("    return this.query.list();");
      out.println("  }");
      out.println();
      out.println("  @Override");
      out.println("  public List<Key<" + entityName + ">> listKeys() {");
      out.println("    return this.query.listKeys();");
      out.println("  }");
      out.println();
      out.println("  @Override");
      out.println("  public " + queryName + " offset(int value) {");
      out.println("    return new " + queryName + "(this.query.offset(value));");
      out.println("  }");
      out.println();
      out.println("  @Override");
      out.println("  public " + queryName + " order(String condition) {");
      out.println("    return new " + queryName
          + "(this.query.order(condition));");
      out.println("  }");
      out.println();
      out.println("  @Override");
      out.println("  public QueryResultIterator<" + entityName
          + "> iterator() {");
      out.println("    return this.query.iterator();");
      out.println("  }");
      out.println();
      out.println("  public ListPage<" + entityName
          + "> list(String cursor, int pageSize) {");
      out.println("    if (cursor != null) {");
      out.println("      this.query.startCursor(Cursor.fromWebSafeString(cursor));");
      out.println("    }");
      out.println("    QueryResultIterator<" + entityName
          + "> iterator = this.query.iterator();");
      out.println("    boolean more = false;");
      out.println("    ArrayList<" + entityName + "> list = new ArrayList<"
          + entityName + ">();");
      out.println("    for (int i = 0; i < pageSize && (more = iterator.hasNext()); i++) {");
      out.println("      list.add(iterator.next());");
      out.println("    }");
      out.println("    return new ListPage<" + entityName
          + ">(list, iterator.getCursor().toWebSafeString(), more);");
      out.println("  }");
      out.println();

      out.println("  public ListPage<Key<" + entityName
          + ">> listKeys(String cursor, int pageSize) {");
      out.println("    if (cursor != null) {");
      out.println("      this.query.startCursor(Cursor.fromWebSafeString(cursor));");
      out.println("    }");

      out.println("    QueryResultIterator<Key<" + entityName
          + ">> iterator = this.query.fetchKeys().iterator();");
      out.println("    boolean more = false;");
      out.println("    ArrayList<Key<" + entityName
          + ">> list = new ArrayList<Key<" + entityName + ">>();");
      out.println("    for (int i = 0; i < pageSize && (more = iterator.hasNext()); i++) {");
      out.println("      list.add(iterator.next());");
      out.println("    }");
      out.println("    return new ListPage<Key<" + entityName
          + ">>(list, iterator.getCursor().toWebSafeString(), more);");
      out.println("  }");

      if (parentField != null) {
        String fieldType = env.getTypeUtils().asMemberOf((DeclaredType)entityElement.asType(), parentField).toString();
        fieldType = fieldType.substring(fieldType.indexOf('<') + 1,
            fieldType.lastIndexOf('>'));

        out.println();
        out.println("  public ListPage<" + fieldType
            + "> listParents(String cursor, int pageSize) {");
        out.println();
        out.println("    if (cursor != null) {");
        out.println("      this.query.startCursor(Cursor.fromWebSafeString(cursor));");
        out.println("    }");
        out.println("    QueryResultIterator<Key<" + entityName
            + ">> iterator = this.query.fetchKeys().iterator();");
        out.println();
        out.println("    boolean more = false;");
        out.println("    ArrayList<Long> idList = new ArrayList<Long>();");
        out.println("    for (int i = 0; i < pageSize && (more = iterator.hasNext()); i++) {");
        out.println("      idList.add(iterator.next().getParent().getId());");
        out.println("    }");
        out.println();
        out.println("    Map<Long, " + fieldType + "> list = ofy().get("
            + fieldType + ".class, idList);");
        out.println("    return new ListPage<" + fieldType + ">(new ArrayList<"
            + fieldType + ">(");
        out.println("      list.values()), iterator.getCursor().toWebSafeString(), more);");
        out.println("  }");
        out.println();
        out.println("  public ListPage<Key<" + fieldType
            + ">> listParentKeys(String cursor, int pageSize) {");
        out.println();
        out.println("    if (cursor != null) {");
        out.println("      this.query.startCursor(Cursor.fromWebSafeString(cursor));");
        out.println("    }");
        out.println("    QueryResultIterator<Key<" + entityName
            + ">> iterator = this.query.fetchKeys().iterator();");
        out.println();
        out.println("    boolean more = false;");
        out.println("    ArrayList<Key<" + fieldType
            + ">> idList = new ArrayList<Key<" + fieldType + ">>();");
        out.println("    for (int i = 0; i < pageSize && (more = iterator.hasNext()); i++) {");
        out.println("      idList.add(new Key<" + fieldType + ">(" + fieldType
            + ".class, iterator.next().getParent().getId()));");
        out.println("    }");
        out.println();
        out.println("    return new ListPage<Key<" + fieldType
            + ">>(idList, iterator.getCursor().toWebSafeString(), more);");
        out.println("  }");
        out.println();
      }

      for (ExecutableElement methodElement : ElementFilter.methodsIn(env.getElementUtils().getAllMembers(
          (TypeElement) entityElement))) {

        if (methodElement.getParameters().size() == 0) {
          List listField = methodElement.getAnnotation(List.class);

          if (listField != null) {

            String returnType = methodElement.getReturnType().toString();
            returnType = returnType.substring(returnType.indexOf('<') + 1,
                returnType.lastIndexOf('>'));
            String idType;
            String idAccessor;
            if (listField.keyType() == KeyType.Id) {
              idType = "Long";
              idAccessor = "getId()";
            } else {
              idType = "String";
              idAccessor = "getName()";
            }

            out.println("  public ListPage<Key<" + returnType + ">> list"
                + listField.singularName()
                + "Keys(String cursor, int pageSize) {");
            out.println();
            out.println("    if (cursor != null) {");
            out.println("      this.query.startCursor(Cursor.fromWebSafeString(cursor));");
            out.println("    }");
            out.println("    QueryResultIterator<" + entityName
                + "> iterator = query.iterator();");
            out.println();
            out.println("    boolean more = false;");
            out.println("    ArrayList<Key<" + returnType
                + ">> idList = new ArrayList<Key<" + returnType + ">>();");
            out.println("    for (int i = 0; i < pageSize && (more = iterator.hasNext()); i++) {");
            out.println("      idList.add(iterator.next()."
                + methodElement.getSimpleName().toString() + "());");
            out.println("    }");
            out.println();
            out.println("    return new ListPage<Key<" + returnType
                + ">>(idList, iterator.getCursor()");
            out.println("        .toWebSafeString(), more);");
            out.println("  }");
            out.println();
            out.println("  public ListPage<" + returnType + "> list"
                + listField.pluralName() + "(String cursor, int pageSize) {");
            out.println("    if (cursor != null) {");
            out.println("      this.query.startCursor(Cursor.fromWebSafeString(cursor));");
            out.println("    }");
            out.println("    QueryResultIterator<" + entityName
                + "> iterator = query.iterator();");
            out.println();
            out.println("    boolean more = false;");
            out.println("    ArrayList<" + idType + "> idList = new ArrayList<"
                + idType + ">();");
            out.println("    for (int i = 0; i < pageSize && (more = iterator.hasNext()); i++) {");
            out.println("      idList.add(iterator.next()."
                + methodElement.getSimpleName().toString() + "()." + idAccessor
                + ");");
            out.println("    }");
            out.println();
            out.println("    Map<" + idType + ", " + returnType + "> list =");
            out.println("      ofy().get(" + returnType + ".class, idList);");
            out.println("    return new ListPage<" + returnType
                + ">(new ArrayList<" + returnType + ">(");
            out.println("      list.values()), iterator.getCursor().toWebSafeString(), more);");
            out.println("  }");
            
          }
        }
      }
      out.println();
      out.println("  protected Objectify ofy() {");
      out.println("    if (this.lazyOfy == null) {");
      out.println("      ObjectifyOpts opts = new ObjectifyOpts().setSessionCache(true);");
      out.println("      this.lazyOfy = ObjectifyService.factory().begin(opts);");
      out.println("    }");
      out.println("    return this.lazyOfy;");
      out.println("  }");
      out.println();

      out.println("}");

    } catch (java.io.IOException e) {
      throw new RuntimeException(e);
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }
}
