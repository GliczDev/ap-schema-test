package me.glicz.schema;

import me.glicz.ap.Schema;

import java.util.List;
@Schema
interface CodeTypeSchema extends AnnotationHolder {
    String fullyQualifiedName();

    @Override
    List<CodeType> annotations();
}
