/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gololang.gradle;

import org.gradle.api.internal.tasks.compile.CompilationFailedException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction

/**
 * @author Yannick Loiseau
 */
class GoloDoc extends SourceTask {

    private static final String GOLO_COMPILER_CLASS_NAME = 'org.eclipse.golo.compiler.GoloCompiler'
    private static final String GOLO_DOCPROCESSOR_CLASS_NAME = 'org.eclipse.golo.doc.HtmlProcessor'
    public static final String GOLO_CLASSPATH_FIELD = 'goloClasspath'
    protected static final String COMPILATION_EXCEPTION_CLASS_NAME = 'org.eclipse.golo.compiler.GoloCompilationException'
    FileCollection goloClasspath

    @TaskAction
    protected void generate() {
        def compiler = instantiateCompiler()
        def processor = instantiateProcessor()
        def units = [:]
        source.files.each { file ->
            try {
                units.put(file.path, compiler.parse(file.path))
            } catch (Exception e) {
                if (e.class.name == COMPILATION_EXCEPTION_CLASS_NAME) {
                    def messages = [e.message, e.cause?.message] + e.problems*.description
                    messages.findAll { it }.each { System.err.println(it) }
                    throw new CompilationFailedException()
                }
                throw e
            }
        }
        processor.process(units, destinationDir)

    }

    protected instantiateCompiler() {
        def goloCompilerClass = loadGoloClass(GOLO_COMPILER_CLASS_NAME)
        goloCompilerClass.getConstructor().newInstance()
    }

    protected instantiateProcessor() {
        def goloHtmlProcessorClass = loadGoloClass(GOLO_DOCPROCESSOR_CLASS_NAME)
        goloHtmlProcessorClass.getConstructor().newInstance()
    }

    protected Class loadGoloClass(String name) {
        def goloClasspathUrls = getGoloClasspath().files.collect { it.toURI().toURL() } as URL[]
        def goloClassLoader = URLClassLoader.newInstance(goloClasspathUrls, getClass().classLoader)
        goloClassLoader.loadClass(name, true)
    }
}
