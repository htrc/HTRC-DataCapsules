SHELL=/bin/bash

LATEX=pdflatex

LFLAGS=-file-line-error -halt-on-error -shell-escape
#LFLAGS=

#QUIET= | grep -iE 'warning|error' -C 2
#SILENT= >/dev/null
QUIET=
SILENT=

TARGET=paper
TARGETBIB=$(wildcard *.bib)
FIGURES=figures
EPSFIGURES=$(wildcard ${FIGURES}/*.eps)
PDFFIGURES=$(EPSFIGURES:%.eps=%-eps-converted-to.pdf)

# make pdf by default
all: ${TARGET}.pdf

# it doesn't really need the .dvi, but this way all the refs are right
%.pdf : %.dvi 
	@${LATEX} ${LFLAGS} $* ${QUIET}

${TARGET}.bbl: ${TARGET}.aux ${TARGETBIB}
# get the citations out of the bibliography
	@bibtex ${TARGET} ${QUIET}
# do it again in case there are out-of-order cross-references
	@${LATEX} ${LFLAGS} ${TARGET}.tex ${SILENT}

${TARGET}.aux: ${TARGET}.tex
# in case we don't already have a .aux file listing citations
	@${LATEX} ${LFLAGS} ${TARGET}.tex ${SILENT}

${TARGET}.dvi: ${TARGET}.bbl ${TARGET}.tex
	@${LATEX} ${LFLAGS} ${TARGET}.tex ${SILENT}

# shortcut, so we can say "make ps"
ps: ${TARGET}.ps

${TARGET}.ps: ${TARGET}.dvi
	@dvips -t a4 ${TARGET}.dvi ${QUIET}

clean:
	@rm -f *.{log,aux,ps,dvi,bbl,blg,log}
	@rm -f ${PDFFIGURES}

reallyclean: clean
	@rm -f ${TARGET}.{ps,pdf}

.PHONY : ps all clean reallyclean
