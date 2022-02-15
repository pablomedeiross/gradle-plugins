package io.freefair.gradle.plugins.lombok.tasks;

import lombok.AccessLevel;
import lombok.Getter;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;
import org.gradle.process.ExecOperations;

import javax.inject.Inject;

/**
 * Runs registered post compiler handlers to against existing class files, modifying them in the process.
 *
 * @author Lars Grefer
 */
@Getter
@CacheableTask
public class PostCompile extends DefaultTask implements LombokTask {

    @Getter(AccessLevel.NONE)
    private final ExecOperations execOperations;

    @Classpath
    private final ConfigurableFileCollection lombokClasspath = getProject().files();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    @SkipWhenEmpty
    private final ConfigurableFileCollection classFiles = getProject().files();

    @Console
    private final Property<Boolean> verbose = getProject().getObjects().property(Boolean.class).convention(false);

    @Inject
    public PostCompile(ExecOperations execOperations) {
        this.execOperations = execOperations;
    }

    @TaskAction
    public void postCompile() {
        execOperations.javaexec(postCompile -> {
            postCompile.setClasspath(getLombokClasspath());
            postCompile.getMainClass().set("lombok.launch.Main");
            postCompile.args("post-compile");

            if (verbose.get()) {
                postCompile.args("--verbose");
            }

            classFiles.forEach(file -> postCompile.args(file.getAbsolutePath()));
        });
    }
}
