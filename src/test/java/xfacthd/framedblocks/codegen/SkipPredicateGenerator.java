package xfacthd.framedblocks.codegen;

import xfacthd.framedblocks.codegen.impl.skippreds.SkipPredicateGeneratorImpl;

import java.util.List;

public final class SkipPredicateGenerator
{
    public static void main(String[] args)
    {
        // The type for which the predicate should be generated (must be defined in SkipPredicateGeneratorData.KNOWN_TYPES)
        String sourceType = "FRAMED_TYPE";
        // The types against which the generated predicate should test (must be defined in SkipPredicateGeneratorData.KNOWN_TYPES)
        List<String> targetTypes = List.of(
                "FRAMED_OTHER_TYPE"
        );

        SkipPredicateGeneratorImpl.generateAndExportClass(sourceType, targetTypes);
    }



    private SkipPredicateGenerator() { }
}
