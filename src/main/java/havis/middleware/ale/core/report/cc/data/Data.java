package havis.middleware.ale.core.report.cc.data;

import havis.middleware.ale.base.operation.tag.Tag;
import havis.middleware.ale.core.Useable;
import havis.middleware.ale.core.field.Bytes;
import havis.middleware.ale.core.field.Characters;
import havis.middleware.ale.core.field.FieldDatatype;
import havis.middleware.ale.core.field.FieldFormat;

interface Data extends Useable {

    Bytes getBytes(Tag tag);

    Characters getCharacters(Tag tag);

    FieldDatatype getFieldDatatype();

    FieldFormat getFieldFormat();
}