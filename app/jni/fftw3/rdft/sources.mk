sources = buffered.c \
          buffered2.c \
          conf.c \
          ct-hc2c-direct.c \
          ct-hc2c.c \
          dft-r2hc.c \
          dht-r2hc.c \
          dht-rader.c \
          direct-r2c.c \
          direct-r2r.c \
          direct2.c \
          generic.c \
          hc2hc-direct.c \
          hc2hc-generic.c \
          hc2hc.c \
          indirect.c \
          khc2c.c \
          khc2hc.c \
          kr2c.c  \
          kr2r.c \
          nop.c \
          nop2.c \
          plan.c \
          plan2.c \
          problem.c \
          problem2.c \
          rank-geq2-rdft2.c \
          rank-geq2.c \
          rank0-rdft2.c \
          rank0.c \
          rdft-dht.c \
          rdft2-inplace-strides.c \
          rdft2-rdft.c \
          rdft2-strides.c \
          rdft2-tensor-max-index.c \
          solve.c \
          solve2.c \
          vrank-geq1-rdft2.c \
          vrank-geq1.c \
          vrank3-transpose.c


    LOCAL_SRC_FILES += $(sources:%=rdft/%)