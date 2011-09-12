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
import javax.persistence.Transient;
import javax.tools.Diagnostic.Kind;

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

      printMessage(Kind.NOTE, "com.googlecode.objectify.query.EntityProcessor started.");
      printMessage(Kind.NOTE, "Searching for @Entity annotations.");    	
      for (Element entity : roundEnv.getElementsAnnotatedWith(Entity.class)) {
        printMessage(Kind.NOTE, "Found " + entity.toString() + ".");
        this.processEntity(entity);
      }
    }

    return false;
  }
  
  private void printMessage(Kind kind, String msg) {
	  this.env.getMessager().printMessage(kind, msg);
  }

  private void processEntity(Element entityElement) {
    PrintWriter out = null;

    String entityPackageName = env.getElementUtils().getPackageOf(entityElement).getQualifiedName().toString();
    String entityName = entityElement.getSimpleName().toString();
    String queryName = entityName + "Query";
    String queryPackageName = entityPackageName.replaceAll("\\.shared\\.",
        ".server.");
    String queryQName = queryPackageName + "." + queryName;
    
    printMessage(Kind.NOTE, "Generating '" + queryName + "' from '" + entityName + "'.");

    Unindexed unindexedClass = entityElement.getAnnotation(Unindexed.class);

    VariableElement parentField = null;

    try {
      out = new PrintWriter(
          new BufferedWriter(this.env.getFiler().createSourceFile(queryQName,
              entityElement).openWriter()));

      // this.env.getMessager().printMessage(Kind.ERROR, entityName);

      out.println("package " + queryPackageName + ";");
      out.println();
      out.println("import java.util.ArrayList;");
      out.println("import java.util.Map;");
      out.println();
      out.println("import com.google.appengine.api.datastore.Cursor;");
      out.println("import com.google.appengine.api.datastore.QueryResultIterator;");
      out.println("import com.googlecode.objectify.Key;");
      out.println("import com.googlecode.objectify.Objectify;");
      out.println("import com.googlecode.objectify.ObjectifyOpts;");
      out.println("import com.googlecode.objectify.ObjectifyService;");
      out.println("import com.googlecode.objectify.Query;");
      try {
        // objectify 3.x
        Class.forName("com.googlecode.objectify.util.QueryWrapper");
        out.println("import com.googlecode.objectify.util.QueryWrapper;");
      } catch (ClassNotFoundException e) {
        try {
          // objectify 2.x
          Class.forName("com.googlecode.objectify.helper.QueryWrapper");
          out.println("import com.googlecode.objectify.helper.QueryWrapper;");
        } catch (ClassNotFoundException e1) {
          env.getMessager().printMessage(Kind.ERROR,
              "Could not find QueryWrapper class");
        } catch (NoClassDefFoundError e2) {
            env.getMessager().printMessage(Kind.ERROR,
                    "Could not find QueryWrapper class");        	
        }
      }
      out.println("import com.googlecode.objectify.query.shared.ListPage;");
      out.println("import " + entityPackageName + "." + entityName + ";");
      out.println();
      out.println("/** Query generated using " + entityPackageName + "." + entityName + " */");
      out.println("public class " + queryName + " extends QueryWrapper<"
          + entityName + "> { ");
      out.println();
      out.println("  private final Query<" + entityName + "> query;");
      out.println("  private Objectify lazyOfy;");
      out.println();
      out.println("  public " + queryName + "(Query<" + entityName
          + "> query) {");
      out.println("    super(query);");
      out.println("    this.query = query;");
      out.println("  }");
      out.println();

      for (VariableElement fieldElement : ElementFilter.fieldsIn(env.getElementUtils().getAllMembers(
          (TypeElement) entityElement))) {

        Unindexed unindexedField = fieldElement.getAnnotation(Unindexed.class);
        Transient transientField = fieldElement.getAnnotation(Transient.class);
        NotSaved notSavedField = fieldElement.getAnnotation(NotSaved.class);
        Indexed indexedField = fieldElement.getAnnotation(Indexed.class);

        Parent parent = fieldElement.getAnnotation(Parent.class);
        if (parentField == null && parent != null) {
          parentField = fieldElement;
        } else {
          if (unindexedField != null && unindexedField.value().length == 0) {
            // @Unindexed field without If... parameter
           continue;
          } else if (notSavedField != null) {
            // @NotSaved field
            continue;
          } else if(transientField != null) {
            // @Transient field
            continue;
          } else if (unindexedClass != null && indexedField == null) {
            // @Unindexed class and field is not @Indexed
            continue;
          }
        }
		
        String fieldName = fieldElement.getSimpleName().toString();
        String fieldType = env.getTypeUtils().asMemberOf(
            (DeclaredType) entityElement.asType(), fieldElement).toString();

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
        String fieldType = env.getTypeUtils().asMemberOf(
            (DeclaredType) entityElement.asType(), parentField).toString();
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
      this.env.getMessager().printMessage(Kind.ERROR, e.toString());
      throw new RuntimeException(e);
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }
}
