package com.github.vbauer.jackdaw.code.base;

import com.github.vbauer.jackdaw.JackdawProcessor;
import com.github.vbauer.jackdaw.context.ProcessorContext;
import com.github.vbauer.jackdaw.context.ProcessorContextHolder;
import com.github.vbauer.jackdaw.util.ProcessorUtils;
import com.github.vbauer.jackdaw.util.SourceCodeUtils;
import com.github.vbauer.jackdaw.util.TypeUtils;
import com.github.vbauer.jackdaw.util.model.ClassType;
import com.google.common.base.Function;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.apache.commons.lang3.time.DateFormatUtils;

import javax.annotation.Generated;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Date;

/**
 * @author Vladislav Bauer
 */

public abstract class GeneratedCodeGenerator extends CodeGenerator {

    protected final Function<String, String> nameModifier;
    protected final ClassType classType;
    protected final String className;


    public GeneratedCodeGenerator(
        final TypeElement typeElement, Function<String, String> nameModifier, final ClassType classType
    ) {
        super(typeElement);
        this.nameModifier = nameModifier;
        this.classType = classType;
        this.className = nameModifier.apply(TypeUtils.getName(typeElement));
    }


    @Override
    public void generate() throws Exception {
        final JavaFileObject file = ProcessorUtils.createSourceFile(typeElement, packageName, className);
        final Writer writer = file.openWriter();
        final PrintWriter printWriter = new PrintWriter(writer);

        final JavaFile javaFile = generateSourceCode();
        final String sourceCode = javaFile.toString();

        printWriter.write(sourceCode);
        printWriter.flush();

        writer.close();
    }


    protected abstract void generateBody(TypeSpec.Builder builder) throws Exception;


    private TypeSpec.Builder generateCommon() {
        final TypeSpec.Builder typeSpecBuilder = createTypeSpecBuilder(classType, className);
        typeSpecBuilder.addModifiers(Modifier.PUBLIC);

        if (classType == ClassType.UTILITY) {
            typeSpecBuilder
                .addModifiers(Modifier.FINAL)
                .addMethod(
                    MethodSpec.constructorBuilder()
                        .addModifiers(Modifier.PRIVATE)
                        .addStatement("throw new $T()", UnsupportedOperationException.class)
                        .build()
                );
        }

        final ProcessorContext processorContext = ProcessorContextHolder.getContext();
        if (processorContext.isAddGeneratedAnnotation()) {
            addGeneratedAnnotation(typeSpecBuilder, processorContext);
        }
        if (processorContext.isAddSuppressWarningsAnnotation()) {
            addSuppressWarningsAnnotation(typeSpecBuilder);
        }

        return typeSpecBuilder;
    }

    private TypeSpec.Builder createTypeSpecBuilder(final ClassType classType, final String className) {
        switch (classType) {
            case ANNOTATION:
                return TypeSpec.annotationBuilder(className);
            case INTERFACE:
                return TypeSpec.interfaceBuilder(className);
            default:
                return TypeSpec.classBuilder(className);
        }
    }

    private void addSuppressWarningsAnnotation(final TypeSpec.Builder typeSpecBuilder) {
        typeSpecBuilder.addAnnotation(
            AnnotationSpec.builder(SuppressWarnings.class)
                .addMember("value", "$S", "all")
                    .build());
    }

    private void addGeneratedAnnotation(
        final TypeSpec.Builder typeSpecBuilder, final ProcessorContext processorContext
    ) {
        final AnnotationSpec.Builder annotationBuilder =
            AnnotationSpec.builder(Generated.class)
                .addMember("value", "$S", JackdawProcessor.class.getName());

        if (processorContext.isAddGeneratedDate()) {
            final String currentTime =
                DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(new Date());

            annotationBuilder.addMember("date", "$S", currentTime);
        }

        typeSpecBuilder.addAnnotation(annotationBuilder.build());
    }

    private JavaFile generateSourceCode() throws Exception {
        final TypeSpec.Builder typeSpecBuilder = generateCommon();
        generateBody(typeSpecBuilder);

        final TypeSpec typeSpec = typeSpecBuilder.build();
        return JavaFile.builder(packageName, typeSpec)
            .indent(SourceCodeUtils.INDENT)
            .skipJavaLangImports(true)
            .build();
    }

}
