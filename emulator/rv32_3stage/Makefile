include ../../prefix.mk

MK_TARGET_PROC  := rv32_3stage
SUPERVISOR_MODE="false"
 
global_asm_tests += \
	rv32mi-p-csr \
	rv32mi-p-illegal \
 

include $(srcDir)/emulator/common/Makefile.include


CHISEL_ARGS := -td emulator/${MK_TARGET_PROC}/generated-src -frsq -c:Top:-o:emulator/${MK_TARGET_PROC}/generated-src/temp.conf 