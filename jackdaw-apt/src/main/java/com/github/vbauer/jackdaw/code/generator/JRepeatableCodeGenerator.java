package com.github.vbauer.jackdaw.code.generator;

import com.github.vbauer.jackdaw.code.base.GeneratedCodeGenerator;
import com.github.vbauer.jackdaw.util.SourceCodeUtils;
import com.github.vbauer.jackdaw.util.TypeUtils;
import com.github.vbauer.jackdaw.util.function.AddSuffix;
import com.github.vbauer.jackdaw.util.model.ClassType;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;
import org.apache.commons.lang3.tuple.Pair;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.List;
import java.util.Map;

/**
 * @author Vladislav Bauer
 */

public class JRepeatableCodeGenerator extends GeneratedCodeGenerator {

    private static final String SUFFIX = "List";
    private static final AddSuffix NAME_MODIFIER = new AddSuffix(SUFFIX);


    public JRepeatableCodeGenerator(final TypeElement typeElement) {
        super(typeElement, NAME_MODIFIER, ClassType.ANNOTATION);
    }


    @Override
    protected void generateBody(final TypeSpec.Builder builder) throws Exception {
        final List<? extends AnnotationMirror> annotations = typeElement.getAnnotationMirrors();

        for (final AnnotationMirror annotation : annotations) {
            final AnnotationSpec.Builder annotationBuilder = AnnotationSpec.builder(
                ClassName.get((TypeElement) annotation.getAnnotationType().asElement())
            );
            final Map<? extends ExecutableElement, ? extends AnnotationValue> params = annotation.getElementValues();

            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : params.entrySet()) {
                final ExecutableElement paramMethod = entry.getKey();
                final String methodName = TypeUtils.getName(paramMethod);

                final AnnotationValue annotationValue = entry.getValue();
                final Object value = annotationValue.getValue();
                final Pair<String, Object> format = calculateValue(value);

                annotationBuilder.addMember(methodName, format.getKey(), format.getValue());
            }

            builder.addAnnotation(annotationBuilder.build());
        }
    }


    private Pair<String, Object> calculateValue(Object value) {
        String format = "L";
        if (value instanceof CharSequence) {
            format = "S";
        } else if (value instanceof Class) {
            format = "T";
        } else if (value instanceof VariableElement) {
            final VariableElement variableElement = (VariableElement) value;
            final ElementKind kind = variableElement.getKind();
            if (kind == ElementKind.ENUM_CONSTANT) {
                final Element classElement = ((VariableElement) value).getEnclosingElement();
                value = classElement + SourceCodeUtils.PACKAGE_SEPARATOR + variableElement;
                format = "L";
            }
        }
        return Pair.of("$" + format, value);
    }

}
