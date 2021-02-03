#!/bin/bash
# clear temp files
rm -f ALE_Vendor_Specification.aux
rm -f ALE_Vendor_Specification.log
rm -f ALE_Vendor_Specification.lot
rm -f ALE_Vendor_Specification.out
rm -f ALE_Vendor_Specification.pdf
rm -f ALE_Vendor_Specification.toc

pdflatex ALE_Vendor_Specification.tex

# run again for table of contents
pdflatex ALE_Vendor_Specification.tex

# run again to correct line numbers
pdflatex ALE_Vendor_Specification.tex

# move pdf
mkdir -p ../classpath/havis/middleware/ale/core/doc
mv ALE_Vendor_Specification.pdf ../classpath/havis/middleware/ale/core/doc/

# cleanup
rm -f ALE_Vendor_Specification.aux
rm -f ALE_Vendor_Specification.log
rm -f ALE_Vendor_Specification.lot
rm -f ALE_Vendor_Specification.out
rm -f ALE_Vendor_Specification.pdf
rm -f ALE_Vendor_Specification.toc
