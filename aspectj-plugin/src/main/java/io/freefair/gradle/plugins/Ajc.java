package io.freefair.gradle.plugins;

import lombok.Getter;
import lombok.Setter;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@CacheableTask
public class Ajc extends DefaultTask {

    @Classpath
    @InputFiles
    private final ConfigurableFileCollection aspectjClasspath = getProject().getObjects().fileCollection();

    /**
     * Accept as source bytecode any .class files in the .jar files or directories on Path.
     * The output will include these classes, possibly as woven with any applicable aspects. Path is a single argument containing a list of paths to zip files or directories, delimited by the platform-specific path delimiter.
     */
    @Classpath
    @InputFiles
    private final ConfigurableFileCollection inpath = getProject().getObjects().fileCollection();

    /**
     * Weave binary aspects from jar files and directories on path into all sources.
     * The aspects should have been output by the same version of the compiler.
     * When running the output classes, the run classpath should contain all aspectpath entries.
     * Path, like classpath, is a single argument containing a list of paths to jar files, delimited by the platform- specific classpath delimiter.
     */
    @Classpath
    @InputFiles
    private final ConfigurableFileCollection aspectpath = getProject().getObjects().fileCollection();

    /**
     * Put output classes in zip file output.jar.
     */
    @Optional
    @OutputFile
    private final RegularFileProperty outjar = getProject().getObjects().fileProperty();

    /**
     * Generate aop xml file for load-time weaving with default name (META-INF/aop-ajc.xml).
     */
    @Input
    private final Property<Boolean> outxml = getProject().getObjects().property(Boolean.class).convention(false);

    /**
     * Generate aop.xml file for load-time weaving with custom name.
     */
    @Optional
    @OutputFile
    private final RegularFileProperty outxmlfile = getProject().getObjects().fileProperty();

    /**
     * Generate a build .ajsym file into the output directory.
     * Used for viewing crosscutting references by tools like the AspectJ Browser.
     */
    @Input
    private final Property<Boolean> crossrefs = getProject().getObjects().property(Boolean.class).convention(false);

    /**
     * Emit the version of the AspectJ compiler.
     */
    @Input
    private final Property<Boolean> version = getProject().getObjects().property(Boolean.class).convention(false);

    /**
     * Specify where to find user class files.
     * Path is a single argument containing a list of paths to zip files or directories, delimited by the platform-specific path delimiter.
     */
    @CompileClasspath
    @InputFiles
    private final ConfigurableFileCollection classpath = getProject().getObjects().fileCollection();

    /**
     * Override location of VM's bootclasspath for purposes of evaluating types when compiling.
     * Path is a single argument containing a list of paths to zip files or directories, delimited by the platform-specific path delimiter.
     */
    @Optional
    @CompileClasspath
    @InputFiles
    private final ConfigurableFileCollection bootclasspath = getProject().getObjects().fileCollection();

    /**
     * Override location of VM's extension directories for purposes of evaluating types when compiling.
     * Path is a single argument containing a list of paths to directories, delimited by the platform-specific path delimiter.
     */
    @InputFiles
    private final ConfigurableFileCollection extdirs = getProject().getObjects().fileCollection();

    /**
     * Specify where to place generated .class files.
     */
    @OutputDirectory
    private final DirectoryProperty destinationDir = getProject().getObjects().directoryProperty();

    @Input
    private final Property<String> target = getProject().getObjects().property(String.class);

    @Input
    private final Property<String> source = getProject().getObjects().property(String.class);

    /**
     * Emit no warnings (equivalent to '-warn:none') This does not suppress messages generated by declare warning or Xlint.
     */
    @Optional
    @Input
    private final Property<Boolean> nowarn = getProject().getObjects().property(Boolean.class).convention(false);

    /**
     * Emit warnings for any instances of the comma-delimited list of questionable code (eg '-warn:unusedLocals,deprecation'):
     *
     *     constructorName        method with constructor name
     *     packageDefaultMethod   attempt to override package-default method
     *     deprecation            usage of deprecated type or member
     *     maskedCatchBlocks      hidden catch block
     *     unusedLocals           local variable never read
     *     unusedArguments        method argument never read
     *     unusedImports          import statement not used by code in file
     *     none                   suppress all compiler warnings
     *
     *
     * -warn:none does not suppress messages generated by declare warning or Xlint.
     */
    @Input
    private final ListProperty<String> warn = getProject().getObjects().listProperty(String.class);

    /**
     * Same as -warn:deprecation
     */
    @Input
    private final Property<Boolean> deprecation = getProject().getObjects().property(Boolean.class).convention(false);

    /**
     * Emit no errors for unresolved imports
     */
    @Input
    private final Property<Boolean> noImportError = getProject().getObjects().property(Boolean.class).convention(false);

    /**
     * Keep compiling after error, dumping class files with problem methods
     */
    @Input
    private final Property<Boolean> proceedOnError = getProject().getObjects().property(Boolean.class).convention(false);

    /**
     * debug attributes level, that may take three forms:
     *
     *     -g         all debug info ('-g:lines,vars,source')
     *     -g:none    no debug info
     *     -g:{items} debug info for any/all of [lines, vars, source], e.g.,
     *                -g:lines,source
     *
     */
    @Input
    private final ListProperty<String> g = getProject().getObjects().listProperty(String.class);

    /**
     * Preserve all local variables during code generation (to facilitate debugging).
     */
    @Input
    private final Property<Boolean> preserveAllLocals = getProject().getObjects().property(Boolean.class).convention(false);

    /**
     * Compute reference information.
     */
    @Input
    private final Property<Boolean> referenceInfo = getProject().getObjects().property(Boolean.class).convention(false);

    /**
     * Specify default source encoding format.
     */
    @Optional
    @Input
    private final Property<String> encoding = getProject().getObjects().property(String.class);


    @Input
    private final Property<Boolean> verbose = getProject().getObjects().property(Boolean.class).convention(false);

    /**
     * Emit messages about weaving
     */
    @Input
    private final Property<Boolean> showWeaveInfo = getProject().getObjects().property(Boolean.class).convention(false);

    /**
     * Specify a log file for compiler messages.
     */
    @OutputFile
    @Optional
    private final RegularFileProperty log = getProject().getObjects().fileProperty();

    /**
     * Show progress (requires -log mode).
     */
    @Input
    private final Property<Boolean> progress = getProject().getObjects().property(Boolean.class).convention(false);

    /**
     * Display speed information.
     */
    @Input
    private final Property<Boolean> time = getProject().getObjects().property(Boolean.class).convention(false);

    /**
     * Causes compiler to terminate before weaving
     */
    @Input
    @Getter(onMethod_ = @Input)
    private final Property<Boolean> XterminateAfterCompilation = getProject().getObjects().property(Boolean.class).convention(false);

    /**
     * Causes the compiler to calculate and add the SerialVersionUID field to any type implementing Serializable that is affected by an aspect.
     * The field is calculated based on the class before weaving has taken place.
     */
    @Input
    @Getter(onMethod_ = @Input)
    private final Property<Boolean> XaddSerialVersionUID = getProject().getObjects().property(Boolean.class).convention(false);

    /**
     * (Experimental) do not inline around advice
     */
    @Input
    @Getter(onMethod_ = @Input)
    private final Property<Boolean> XnoInline = getProject().getObjects().property(Boolean.class).convention(false);

    /**
     * (Experimental) Normally it is an error to declare aspects Serializable. This option removes that restriction.
     */
    @Input
    @Getter(onMethod_ = @Input)
    private final Property<Boolean> XserializableAspects = getProject().getObjects().property(Boolean.class).convention(false);

    /**
     * (Experimental) Create class files that can't be subsequently rewoven by AspectJ.
     */
    @Input
    @Getter(onMethod_ = @Input)
    private final Property<Boolean> XnotReweavable = getProject().getObjects().property(Boolean.class).convention(false);

    @Optional
    @InputFiles
    private final ConfigurableFileCollection files = getProject().getObjects().fileCollection();

    @TaskAction
    public void ajc() throws IOException {

        List<String> ajcArgs = new LinkedList<>();
        File argfile = new File(getTemporaryDir(), "ajc.options");

        if (!inpath.isEmpty()) {
            ajcArgs.add("-inpath");
            ajcArgs.add(inpath.getAsPath());
        }

        if (!aspectpath.isEmpty()) {
            ajcArgs.add("-aspectpath");
            ajcArgs.add(aspectpath.getAsPath());
        }

        if (outjar.isPresent()) {
            ajcArgs.add("-outjar");
            ajcArgs.add(outjar.get().getAsFile().getAbsolutePath());
        }

        if (outxml.getOrElse(false)) {
            ajcArgs.add("-outxml");
        }

        if (outxmlfile.isPresent()) {
            ajcArgs.add("-outxmlfile");
            ajcArgs.add(outxmlfile.get().getAsFile().getAbsolutePath());
        }

        if (crossrefs.getOrElse(false)) {
            ajcArgs.add("-crossrefs");
        }

        if (version.getOrElse(false)) {
            ajcArgs.add("-version");
        }

        if (!classpath.isEmpty()) {
            ajcArgs.add("-classpath");
            ajcArgs.add(classpath.getAsPath());
        }

        if (!bootclasspath.isEmpty()) {
            ajcArgs.add("-bootclasspath");
            ajcArgs.add(bootclasspath.getAsPath());
        }

        if (!extdirs.isEmpty()) {
            ajcArgs.add("-extdirs");
            ajcArgs.add(extdirs.getAsPath());
        }

        if (destinationDir.isPresent()) {
            ajcArgs.add("-d");
            ajcArgs.add(destinationDir.get().getAsFile().getAbsolutePath());
        }

        if (target.isPresent()) {
            ajcArgs.add("-target");
            ajcArgs.add(target.get());
        }

        if (source.isPresent()) {
            ajcArgs.add("-source");
            ajcArgs.add(source.get());
        }

        if (nowarn.getOrElse(false)) {
            ajcArgs.add("-nowarn");
        }

        if (!warn.getOrElse(Collections.emptyList()).isEmpty()) {
            ajcArgs.add("-warn:" + warn.get().stream().collect(Collectors.joining(",")));
        }

        if (deprecation.getOrElse(false)) {
            ajcArgs.add("-deprecation");
        }

        if (noImportError.getOrElse(false)) {
            ajcArgs.add("-noImportError");
        }

        if (proceedOnError.getOrElse(false)) {
            ajcArgs.add("-proceedOnError");
        }

        if (!g.getOrElse(Collections.emptyList()).isEmpty()) {
            ajcArgs.add("-g:" + g.get().stream().collect(Collectors.joining(",")));
        }

        if (preserveAllLocals.getOrElse(false)) {
            ajcArgs.add("-preserveAllLocals");
        }

        if (referenceInfo.getOrElse(false)) {
            ajcArgs.add("-referenceInfo");
        }

        if (encoding.isPresent()) {
            ajcArgs.add("-encoding");
            ajcArgs.add(encoding.get());
        }

        if (verbose.getOrElse(false)) {
            ajcArgs.add("-verbose");
        }

        if (showWeaveInfo.getOrElse(false)) {
            ajcArgs.add("-showWeaveInfo");
        }

        if (log.isPresent()) {
            ajcArgs.add("-log");
            ajcArgs.add(log.get().getAsFile().getAbsolutePath());
        }

        if (progress.getOrElse(false)) {
            ajcArgs.add("-progress");
        }

        if (time.getOrElse(false)) {
            ajcArgs.add("-time");
        }

        if (XterminateAfterCompilation.getOrElse(false)) {
            ajcArgs.add("-XterminateAfterCompilation");
        }

        if (XaddSerialVersionUID.getOrElse(false)) {
            ajcArgs.add("-XaddSerialVersionUID");
        }

        if (XnoInline.getOrElse(false)) {
            ajcArgs.add("-XnoInline");
        }

        if (XserializableAspects.getOrElse(false)) {
            ajcArgs.add("-XserializableAspects");
        }

        if (XnotReweavable.getOrElse(false)) {
            ajcArgs.add("-XnotReweavable");
        }

        if (!files.isEmpty()) {
            for (File file : files.getFiles()) {
                ajcArgs.add(file.getAbsolutePath());
            }
        }

        Files.write(argfile.toPath(), ajcArgs);

        getProject().javaexec(ajc -> {
            ajc.setClasspath(aspectjClasspath);
            ajc.setMain("org.aspectj.tools.ajc.Main");

            ajc.args("-argfile", argfile);
        });
    }
}
