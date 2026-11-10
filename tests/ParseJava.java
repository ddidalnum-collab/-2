import javax.tools.*;
import com.sun.source.util.JavacTask;
import java.io.File;
import java.util.*;
public class ParseJava {
  public static void main(String[] args) throws Exception {
    JavaCompiler c=ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diags=new DiagnosticCollector<>();
    StandardJavaFileManager fm=c.getStandardFileManager(diags,null,null);
    List<File> files=new ArrayList<>();
    for(String a:args)files.add(new File(a));
    Iterable<? extends JavaFileObject> jfo=fm.getJavaFileObjectsFromFiles(files);
    JavacTask task=(JavacTask)c.getTask(null,fm,diags,List.of("-proc:none"),null,jfo);
    int units=0;for(var unit:task.parse())units++;
    int errors=0;
    for(var d:diags.getDiagnostics())if(d.getKind()==Diagnostic.Kind.ERROR){errors++;System.err.println(d);}
    fm.close();if(errors>0)System.exit(1);
    System.out.println("JAVA_PARSE_PASS: "+units+" translation units; Android type resolution/build NOT tested");
  }
}
