; Taken from https://github.com/halgari/mjolnir/blob/5b1c0cf1c34d5521438ee33974715d98abb7884d/src/mjolnir/llvmc.clj
(ns clj-llvm.llvm.interop
  (:import (com.sun.jna Pointer))
  (:require [clj-llvm.native-interop :refer [with-lib
                                             def-native-fn
                                             def-enum]]))


(with-lib LLVM-3.4
  (def-native-fn Integer LLVMAddBBVectorizePass)
  (def-native-fn Integer LLVMAddCFGSimplificationPass)
  (def-native-fn Integer LLVMAddConstantPropagationPass)
  (def-native-fn Pointer LLVMAddFunction)
  (def-native-fn Pointer LLVMAddFunctionInliningPass)
  (def-native-fn Integer LLVMAddGVNPass)
  (def-native-fn Pointer LLVMAddGlobal)
  (def-native-fn Integer LLVMAddIncoming)
  (def-native-fn Integer LLVMAddInstructionCombiningPass)
  (def-native-fn Integer LLVMAddLoopUnrollPass)
  (def-native-fn Integer LLVMAddLoopVectorizePass)
  (def-native-fn Integer LLVMAddPromoteMemoryToRegisterPass)
  (def-native-fn Pointer LLVMAppendBasicBlock)
  (def-native-fn Pointer LLVMArrayType)
  (def-native-fn Pointer LLVMBuildAdd)
  (def-native-fn Pointer LLVMBuildAdd)
  (def-native-fn Pointer LLVMBuildAlloca)
  (def-native-fn Pointer LLVMBuildAnd)
  (def-native-fn Pointer LLVMBuildArrayMalloc)
  (def-native-fn Pointer LLVMBuildAtomicRMW)
  (def-native-fn Pointer LLVMBuildBinOp)
  (def-native-fn Pointer LLVMBuildBitCast)
  (def-native-fn Pointer LLVMBuildBr)
  (def-native-fn Pointer LLVMBuildCall)
  (def-native-fn Pointer LLVMBuildCast)
  (def-native-fn Pointer LLVMBuildCondBr)
  (def-native-fn Pointer LLVMBuildExtractElement)
  (def-native-fn Pointer LLVMBuildFAdd)
  (def-native-fn Pointer LLVMBuildFCmp)
  (def-native-fn Pointer LLVMBuildFDiv)
  (def-native-fn Pointer LLVMBuildFMul)
  (def-native-fn Pointer LLVMBuildFPToSI)
  (def-native-fn Pointer LLVMBuildFSub)
  (def-native-fn Pointer LLVMBuildFree)
  (def-native-fn Pointer LLVMBuildGEP)
  (def-native-fn Pointer LLVMBuildICmp)
  (def-native-fn Pointer LLVMBuildInBoundsGEP)
  (def-native-fn Pointer LLVMBuildInsertElement)
  (def-native-fn Pointer LLVMBuildIntCast)
  (def-native-fn Pointer LLVMBuildLShr)
  (def-native-fn Pointer LLVMBuildLoad)
  (def-native-fn Pointer LLVMBuildMalloc)
  (def-native-fn Pointer LLVMBuildMul)
  (def-native-fn Pointer LLVMBuildNot)
  (def-native-fn Pointer LLVMBuildOr)
  (def-native-fn Pointer LLVMBuildPhi)
  (def-native-fn Integer LLVMBuildRet)
  (def-native-fn Integer LLVMBuildRetVoid)
  (def-native-fn Pointer LLVMBuildSExt)
  (def-native-fn Pointer LLVMBuildSExtOrBitCast)
  (def-native-fn Pointer LLVMBuildSIToFP)
  (def-native-fn Pointer LLVMBuildShl)
  (def-native-fn Pointer LLVMBuildStore)
  (def-native-fn Pointer LLVMBuildSub)
  (def-native-fn Pointer LLVMBuildSub)
  (def-native-fn Pointer LLVMBuildTrunc)
  (def-native-fn Pointer LLVMBuildTruncOrBitCast)
  (def-native-fn Pointer LLVMBuildZExt)
  (def-native-fn Pointer LLVMBuildZExtOrBitCast)
  (def-native-fn Pointer LLVMConstArray)
  (def-native-fn Pointer LLVMConstBitCast)
  (def-native-fn Pointer LLVMConstGEP)
  (def-native-fn Pointer LLVMConstInt)
  (def-native-fn Pointer LLVMConstInt)
  (def-native-fn Pointer LLVMConstNull)
  (def-native-fn Pointer LLVMConstPointerCast)
  (def-native-fn Pointer LLVMConstPointerNull)
  (def-native-fn Pointer LLVMConstReal)
  (def-native-fn Pointer LLVMConstStruct)
  (def-native-fn Pointer LLVMConstTrunc)
  (def-native-fn Pointer LLVMConstVector)
  (def-native-fn Pointer LLVMConstZExt)
  (def-native-fn Integer LLVMCountParamTypes)
  (def-native-fn Pointer LLVMCreateBuilder)
  (def-native-fn Integer LLVMCreateInterpreterForModule)
  (def-native-fn Integer LLVMCreateJITCompiler)
  (def-native-fn Pointer LLVMCreateModuleProviderForExistingModule)
  (def-native-fn Pointer LLVMCreatePassManager)
  (def-native-fn Pointer LLVMCreateTargetData)
  (def-native-fn Pointer LLVMCreateTargetMachine)
  (def-native-fn Integer LLVMDisposeBuilder)
  (def-native-fn Integer LLVMDisposeExecutionEngine)
  (def-native-fn Integer LLVMDisposeGenericValue)
  (def-native-fn Integer LLVMDisposeMessage)
  (def-native-fn Integer LLVMDisposePassManager)
  (def-native-fn Pointer LLVMDoubleType)
  (def-native-fn Integer LLVMDumpModule)
  (def-native-fn Pointer LLVMFloatType)
  (def-native-fn Pointer LLVMFunctionType)
  (def-native-fn Pointer LLVMGetDefaultTargetTriple)
  (def-native-fn Pointer LLVMGetFirstTarget)
  (def-native-fn Pointer LLVMGetNamedFunction)
  (def-native-fn Pointer LLVMGetNamedGlobal)
  (def-native-fn Pointer LLVMGetNextTarget)
  (def-native-fn Pointer LLVMGetParam)
  (def-native-fn String  LLVMGetTarget)
  (def-native-fn String  LLVMGetTargetDescription)
  (def-native-fn Pointer LLVMGetTargetMachineData)
  (def-native-fn String  LLVMGetTargetMachineTriple)
  (def-native-fn String  LLVMGetTargetName)
  (def-native-fn Integer LLVMGetTypeKind)
  (def-native-fn Integer LLVMGetTypeKind)
  (def-native-fn Integer LLVMInitializeX86AsmParser)
  (def-native-fn Integer LLVMInitializeX86AsmPrinter)
  (def-native-fn Integer LLVMInitializeX86Target)
  (def-native-fn Integer LLVMInitializeX86TargetInfo)
  (def-native-fn Integer LLVMInitializeX86TargetMC)
  (def-native-fn Pointer LLVMInt64Type)
  (def-native-fn Pointer LLVMIntType)
  (def-native-fn Integer LLVMIsConstant)
  (def-native-fn Integer LLVMLinkInInterpreter)
  (def-native-fn Integer LLVMLinkInJIT)
  (def-native-fn Integer LLVMLinkInJIT)
  (def-native-fn Pointer LLVMModuleCreateWithName)
  (def-native-fn Pointer LLVMPointerType)
  (def-native-fn Integer LLVMPositionBuilderAtEnd)
  (def-native-fn Pointer LLVMPrintModuleToString)
  (def-native-fn Integer LLVMRunPassManager)
  (def-native-fn Pointer LLVMSetDataLayout)
  (def-native-fn Integer LLVMSetFunctionCallConv)
  (def-native-fn Integer LLVMSetInitializer)
  (def-native-fn Integer LLVMSetLinkage)
  (def-native-fn Integer LLVMSetTarget)
  (def-native-fn Pointer LLVMStructType)
  (def-native-fn Boolean LLVMTargetHasAsmBackend)
  (def-native-fn Boolean LLVMTargetHasJIT)
  (def-native-fn Boolean LLVMTargetHasTargetMachine)
  (def-native-fn Boolean LLVMTargetMachineEmitToFile)
  (def-native-fn Pointer LLVMTypeOf)
  (def-native-fn Pointer LLVMVectorType)
  (def-native-fn Boolean LLVMVerifyModule)
  (def-native-fn Pointer LLVMVoidType)


  (def LLVMAbortProcessAction 0)
  (def LLVMPrintMessageAction 1)
  (def LLVMReturnStatusAction 2)

  (def LLVMCCallConv 0)
  (def LLVMFastCallConv 8)
  (def LLVMColdCallConv 9)
  (def LLVMX86StdcallCallConv 64)
  (def LLVMX86FastcallCallConv 65)

  (def LLVMPTXGlobal 71)
  (def LLVMPTXDevice 72)



  (def-enum LLVMTypeKind
    [LLVMVoidTypeKind
     LLVMHalfTypeKind
     LLVMFloatTypeKind
     LLVMDoubleTypeKind
     LLVMX86_FP80TypeKind
     LLVMFP128TypeKind
     LLVMPPC_FP128TypeKind
     LLVMLabelTypeKind
     LLVMIntegerTypeKind
     LLVMFunctionTypeKind
     LLVMStructTypeKind
     LLVMArrayTypeKind
     LLVMPointerTypeKind
     LLVMVectorTypeKind
     LLVMMetadataTypeKind
     LLVMX86_MMXTypeKind])

  (def-enum LLVMCodeGentFileType
    [LLVMAssemblyFile
     LLVMObjectFile])

  (def-enum LLVMRelocMode
    [LLVMRelocDefault
     LLVMRelocStatic
     LLVMRelocPIC
     LLVMRelocDynamicNoPIC])

  (def-enum LLVMCodeGenOptLevel
    [LLVMCodeGenLevelNone
     LLVMCodeGenLevelLess
     LLVMCodeGenLevelDefault
     LLVMCodeGenLevelAggressive])

  (def-enum LLVMCodeModel
    [LLVMCodeModelDefault
     LLVMCodeModelJITDefault
     LLVMCodeModelSmall
     LLVMCodeModelKernel
     LLVMCodeModelMedium
     LLVMCodeModelLarge])


  (def-enum LLVMLinkage
    [LLVMExternalLinkage
     LLVMAvailableExternallyLinkage
     LLVMLinkOnceAnyLinkage
     LLVMLinkOnceODRLinkage
     LLVMWeakAnyLinkage
     LLVMWeakODRLinkage
     LLVMAppendingLinkage
     LLVMInternalLinkage
     LLVMPrivateLinkage
     LLVMDLLImportLinkage
     LLVMDLLExportLinkage
     LLVMExternalWeakLinkage
     LLVMGhostLinkage
     LLVMCommonLinkage
     LLVMLinkerPrivateLinkage
     LLVMLinkerPrivateWeakLinkage
     LLVMLinkerPrivateWeakDefAutoLinkage])

  (def-enum LLVMIntPredicate
    32
    [LLVMIntEQ
     LLVMIntNE
     LLVMIntUGT
     LLVMIntUGE
     LLVMIntULT
     LLVMIntULE
     LLVMIntSGT
     LLVMIntSGE
     LLVMIntSLT
     LLVMIntSLE])

  (def-enum LLVMRealPredicate
    [LLVMRealPredicateFalse
     LLVMRealOEQ
     LLVMRealOGT
     LLVMRealOGE
     LLVMRealOLT
     LLVMRealOLE
     LLVMRealONE
     LLVMRealORD
     LLVMRealUNO
     LLVMRealUEQ
     LLVMRealUGT
     LLVMRealUGE
     LLVMRealULT
     LLVMRealULE
     LLVMRealUNE
     LLVMRealPredicateTrue])

  (def-enum LLVMOpcode
    1
    [LLVMRet
     LLVMBr
     LLVMSwitch
     LLVMIndirectBr
     LLVMInvoke
     _Removed
     LLVMUnreachable
     LLVMAdd
     LLVMFAdd
     LLVMSub
     LLVMFSub
     LLVMMul
     LLVMFMul
     LLVMUDiv
     LLVMSDiv
     LLVMFDiv
     LLVMURem
     LLVMSRem
     LLVMFRem
     LLVMShl
     LLVMLShr
     LLVMAShr
     LLVMAnd
     LLVMOr
     LLVMXor
     LLVMAlloca
     LLVMLoad
     LLVMStore
     LLVMGetElementPtr
     LLVMTrunc
     LLVMZExt
     LLVMSExt
     LLVMFPToUI
     LLVMFPToSI
     LLVMUIToFP
     LLVMSIToFP
     LLVMFPTrunc
     LLVMFPExt
     LLVMPtrToInt
     LLVMIntToPtr
     LLVMBitcast
     LLVMICmp
     LLVMFCmp
     LLVMPHI
     LLVMCall
     LLVMSelect
     LLVMUserOp1
     LLVMUserOp2
     LLVMVAArg
     LLVMExtractElement
     LLVMInsertElement
     LLVMShuffleVector
     LLVMExtractValue
     LLVMInsertValue
     LLVMFence
     LLVMAtomicCmpXchg
     LLVMAtomicRMW
     LLVMResume
     LLVMLandingPad])

  (def-enum LLVMAtomicOrdering
    [LLVMAtomicOrderingNotAtomic
    LLVMAtomicOrderingUnordered
    LLVMAtomicOrderingMonotonic
    __
    LLVMAtomicOrderingAcquire
    LLVMAtomicOrderingRelease
    LLVMAtomicOrderingAcquireRelease
    LLVMAtomicOrderingSequentiallyConsistent])

  (def-enum LLVMAtomicRMWBinOp
    [LLVMAtomicRMWBinOpXchg
     LLVMAtomicRMWBinOpAdd
     LLVMAtomicRMWBinOpSub
     LLVMAtomicRMWBinOpAnd
     LLVMAtomicRMWBinOpNand
     LLVMAtomicRMWBinOpOr
     LLVMAtomicRMWBinOpXor
     LLVMAtomicRMWBinOpMax
     LLVMAtomicRMWBinOpMin
     LLVMAtomicRMWBinOpUMax
     LLVMAtomicRMWBinOpUMin]))



(defn target-info [t]
  {:target   t
   :name     (LLVMGetTargetName t)
   :desc     (LLVMGetTargetDescription t)
   :jit?     (LLVMTargetHasJIT t)
   :machine? (LLVMTargetHasTargetMachine t)
   :asm?     (LLVMTargetHasAsmBackend t)})

(defn target-seq
  ([]
    (when-let [first (LLVMGetFirstTarget)]
      (cons (target-info first)
            (lazy-seq (target-seq first)))))
  ([target]
    (when-let [next (LLVMGetNextTarget target)]
      (cons (target-info next)
            (lazy-seq (target-seq next))))))
