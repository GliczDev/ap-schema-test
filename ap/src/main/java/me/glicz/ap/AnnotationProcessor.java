package me.glicz.ap;

import net.strokkur.jap.code.CodeGenUtil;
import net.strokkur.jap.code.classmodel.CodeMethod;
import net.strokkur.jap.code.classmodel.CodeParameterDefinition;
import net.strokkur.jap.code.classmodel.CodeRecord;
import net.strokkur.jap.code.classmodel.builder.RecordBuilder;
import net.strokkur.jap.code.convert.ConvertToExpression;
import net.strokkur.jap.code.expression.Expressions;
import net.strokkur.jap.code.statement.Statements;
import net.strokkur.jap.code.type.CodeClassType;
import net.strokkur.jap.code.type.CodeTypes;
import net.strokkur.jap.code.util.Modifiers;
import net.strokkur.jap.source.SourceMapProcessor;
import net.strokkur.jap.source.SourceMapUtil;
import net.strokkur.jap.source.classmodel.SourceClassLike;
import net.strokkur.jap.source.classmodel.SourceInterface;
import net.strokkur.jap.source.classmodel.SourceMethod;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.Set;

public class AnnotationProcessor extends AbstractProcessor implements SourceMapProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }

        SourceMapUtil mapUtil = new SourceMapUtil(this);
        CodeGenUtil genUtil = new CodeGenUtil(this);

        for (Element element : roundEnv.getElementsAnnotatedWith(Schema.class)) {
            SourceClassLike classLike = mapUtil.parseClassElement((TypeElement) element);
            if (!(classLike instanceof SourceInterface schema)) {
                messager().errorSource("Not an interface", classLike);
                continue;
            }

            if (!schema.classType().name().endsWith("Schema")) {
                messager().errorSource("Missing 'Schema' prefix", schema);
                continue;
            }

            String schemaFqn = schema.classType().fullyQualifiedName();
            CodeClassType classType = CodeTypes.of(schemaFqn.substring(0, schemaFqn.length() - "Schema".length()));

            RecordBuilder recordBuilder = CodeRecord.builder(classType)
                    .addModifiers(Modifiers.PUBLIC)
                    .implementsInterfaces(schema);

            for (SourceMethod method : schema.methods()) {
                recordBuilder.addComponent(method.returnType(), method.name());

                CodeParameterDefinition param1 = CodeParameterDefinition.of(
                        method.returnType(), method.name()
                );

                recordBuilder.addMethods(CodeMethod.builder("with" + Character.toUpperCase(method.name().charAt(0)) + method.name().substring(1))
                        .addModifiers(Modifiers.PUBLIC)
                        .setReturnType(classType)
                        .addParameters(param1)
                        .addCode(Statements.returnStmt(classType.ctor(schema.methods().stream()
                                .map(m -> Expressions.fieldAccess(m.name()))
                                .toArray(ConvertToExpression[]::new)
                        )))
                );
            }

            try {
                genUtil.printJavaFile(recordBuilder.toRecord(), element);
            } catch (IOException e) {
                messager().errorSource("Failed to generate", schema);
            }
        }

        return false;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(Schema.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public ProcessingEnvironment processingEnv() {
        return processingEnv;
    }
}
