SHELL= /bin/bash

LATEX= pdflatex

LFLAGS= -file-line-error -halt-on-error -shell-escape
#LFLAGS=

Q=1
QUIET= $(if $(Q),2>&1 | grep -iE "warning|error" -C 2 || true)
SILENT= $(if $(Q),2>&1 >/dev/null)

TARGET= paper
TARGETBIB= $(wildcard *.bib)
FIGURES= figures
EPSFIGURES= $(wildcard ${FIGURES}/*.eps)
PDFFIGURES= $(EPSFIGURES:%.eps=%-eps-converted-to.pdf)

# make pdf by default
all: ${TARGET}.pdf

${TARGET}.pdf: ${TARGET}.bbl ${TARGET}.tex
	@echo "### Compiling PDF"
	@${LATEX} ${LFLAGS} ${TARGET} ${QUIET}

${TARGET}.bbl: ${TARGET}.aux ${TARGETBIB}
	@echo "### Compiling Bibliography"
# get the citations out of the bibliography
	@bibtex ${TARGET} ${QUIET}
# do it again in case there are out-of-order cross-references
	@${LATEX} ${LFLAGS} ${TARGET}.tex ${SILENT}

# in case we don't already have a .aux file listing citations
${TARGET}.aux: ${TARGET}.tex
	@echo "### Compiling .aux File"
	@${LATEX} ${LFLAGS} ${TARGET}.tex ${SILENT}

clean:
	@rm -f *.{log,aux,ps,dvi,bbl,blg,log}
	@rm -f ${PDFFIGURES}

reallyclean: clean
	@rm -f ${TARGET}.{ps,pdf}

.PHONY : all clean reallyclean
