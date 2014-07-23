; Taken from https://github.com/halgari/mjolnir/blob/5b1c0cf1c34d5521438ee33974715d98abb7884d/src/mjolnir/llvmc.clj
(ns clj-llvm.llvm.interop
  (:import (com.sun.jna Pointer))
  (:require [clj-llvm.native-interop :refer [with-lib
                                             defnative
                                             defenum]]))


(with-lib LLVM-3.4
  (defnative Integer LLVMAddBBVectorizePass)
  (defnative Integer LLVMAddCFGSimplificationPass)
  (defnative Integer LLVMAddConstantPropagationPass)
  (defnative Pointer LLVMAddFunction)
  (defnative Pointer LLVMAddFunctionInliningPass)
  (defnative Integer LLVMAddGVNPass)
  (defnative Pointer LLVMAddGlobal)
  (defnative Integer LLVMAddIncoming)
  (defnative Integer LLVMAddInstructionCombiningPass)
  (defnative Integer LLVMAddLoopUnrollPass)
  (defnative Integer LLVMAddLoopVectorizePass)
  (defnative Integer LLVMAddPromoteMemoryToRegisterPass)
  (defnative Pointer LLVMAppendBasicBlock)
  (defnative Pointer LLVMArrayType)
  (defnative Pointer LLVMBuildAdd)
  (defnative Pointer LLVMBuildAdd)
  (defnative Pointer LLVMBuildAlloca)
  (defnative Pointer LLVMBuildAnd)
  (defnative Pointer LLVMBuildArrayMalloc)
  (defnative Pointer LLVMBuildAtomicRMW)
  (defnative Pointer LLVMBuildBinOp)
  (defnative Pointer LLVMBuildBitCast)
  (defnative Pointer LLVMBuildBr)
  (defnative Pointer LLVMBuildCall)
  (defnative Pointer LLVMBuildCast)
  (defnative Pointer LLVMBuildCondBr)
  (defnative Pointer LLVMBuildExtractElement)
  (defnative Pointer LLVMBuildFAdd)
  (defnative Pointer LLVMBuildFCmp)
  (defnative Pointer LLVMBuildFDiv)
  (defnative Pointer LLVMBuildFMul)
  (defnative Pointer LLVMBuildFPToSI)
  (defnative Pointer LLVMBuildFSub)
  (defnative Pointer LLVMBuildFree)
  (defnative Pointer LLVMBuildGEP)
  (defnative Pointer LLVMBuildICmp)
  (defnative Pointer LLVMBuildInBoundsGEP)
  (defnative Pointer LLVMBuildInsertElement)
  (defnative Pointer LLVMBuildIntCast)
  (defnative Pointer LLVMBuildLShr)
  (defnative Pointer LLVMBuildLoad)
  (defnative Pointer LLVMBuildMalloc)
  (defnative Pointer LLVMBuildMul)
  (defnative Pointer LLVMBuildNot)
  (defnative Pointer LLVMBuildOr)
  (defnative Pointer LLVMBuildPhi)
  (defnative Integer LLVMBuildRet)
  (defnative Integer LLVMBuildRetVoid)
  (defnative Pointer LLVMBuildSExt)
  (defnative Pointer LLVMBuildSExtOrBitCast)
  (defnative Pointer LLVMBuildSIToFP)
  (defnative Pointer LLVMBuildShl)
  (defnative Pointer LLVMBuildStore)
  (defnative Pointer LLVMBuildSub)
  (defnative Pointer LLVMBuildSub)
  (defnative Pointer LLVMBuildTrunc)
  (defnative Pointer LLVMBuildTruncOrBitCast)
  (defnative Pointer LLVMBuildZExt)
  (defnative Pointer LLVMBuildZExtOrBitCast)
  (defnative Pointer LLVMConstArray)
  (defnative Pointer LLVMConstBitCast)
  (defnative Pointer LLVMConstGEP)
  (defnative Pointer LLVMConstInt)
  (defnative Pointer LLVMConstInt)
  (defnative Pointer LLVMConstNull)
  (defnative Pointer LLVMConstPointerCast)
  (defnative Pointer LLVMConstPointerNull)
  (defnative Pointer LLVMConstReal)
  (defnative Pointer LLVMConstStruct)
  (defnative Pointer LLVMConstTrunc)
  (defnative Pointer LLVMConstVector)
  (defnative Pointer LLVMConstZExt)
  (defnative Integer LLVMCountParamTypes)
  (defnative Pointer LLVMCreateBuilder)
  (defnative Integer LLVMCreateInterpreterForModule)
  (defnative Integer LLVMCreateJITCompiler)
  (defnative Pointer LLVMCreateModuleProviderForExistingModule)
  (defnative Pointer LLVMCreatePassManager)
  (defnative Pointer LLVMCreateTargetData)
  (defnative Pointer LLVMCreateTargetMachine)
  (defnative Integer LLVMDisposeBuilder)
  (defnative Integer LLVMDisposeExecutionEngine)
  (defnative Integer LLVMDisposeGenericValue)
  (defnative Integer LLVMDisposeMessage)
  (defnative Integer LLVMDisposePassManager)
  (defnative Pointer LLVMDoubleType)
  (defnative Integer LLVMDumpModule)
  (defnative Pointer LLVMFloatType)
  (defnative Pointer LLVMFunctionType)
  (defnative Pointer LLVMGetDefaultTargetTriple)
  (defnative Pointer LLVMGetFirstTarget)
  (defnative Pointer LLVMGetNamedFunction)
  (defnative Pointer LLVMGetNamedGlobal)
  (defnative Pointer LLVMGetNextTarget)
  (defnative Pointer LLVMGetParam)
  (defnative String  LLVMGetTarget)
  (defnative String  LLVMGetTargetDescription)
  (defnative Pointer LLVMGetTargetMachineData)
  (defnative String  LLVMGetTargetMachineTriple)
  (defnative String  LLVMGetTargetName)
  (defnative Integer LLVMGetTypeKind)
  (defnative Integer LLVMGetTypeKind)
  (defnative Integer LLVMInitializeX86AsmParser)
  (defnative Integer LLVMInitializeX86AsmPrinter)
  (defnative Integer LLVMInitializeX86Target)
  (defnative Integer LLVMInitializeX86TargetInfo)
  (defnative Integer LLVMInitializeX86TargetMC)
  (defnative Pointer LLVMInt64Type)
  (defnative Pointer LLVMIntType)
  (defnative Integer LLVMIsConstant)
  (defnative Integer LLVMLinkInInterpreter)
  (defnative Integer LLVMLinkInJIT)
  (defnative Integer LLVMLinkInJIT)
  (defnative Pointer LLVMModuleCreateWithName)
  (defnative Pointer LLVMPointerType)
  (defnative Integer LLVMPositionBuilderAtEnd)
  (defnative Pointer LLVMPrintModuleToString)
  (defnative Integer LLVMRunPassManager)
  (defnative Pointer LLVMSetDataLayout)
  (defnative Integer LLVMSetFunctionCallConv)
  (defnative Integer LLVMSetInitializer)
  (defnative Integer LLVMSetLinkage)
  (defnative Integer LLVMSetTarget)
  (defnative Pointer LLVMStructType)
  (defnative Boolean LLVMTargetHasAsmBackend)
  (defnative Boolean LLVMTargetHasJIT)
  (defnative Boolean LLVMTargetHasTargetMachine)
  (defnative Boolean LLVMTargetMachineEmitToFile)
  (defnative Pointer LLVMTypeOf)
  (defnative Pointer LLVMVectorType)
  (defnative Boolean LLVMVerifyModule)
  (defnative Pointer LLVMVoidType)


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



  (defenum LLVMTypeKind
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

  (defenum LLVMCodeGentFileType
    [LLVMAssemblyFile
     LLVMObjectFile])

  (defenum LLVMRelocMode
    [LLVMRelocDefault
     LLVMRelocStatic
     LLVMRelocPIC
     LLVMRelocDynamicNoPIC])

  (defenum LLVMCodeGenOptLevel
    [LLVMCodeGenLevelNone
     LLVMCodeGenLevelLess
     LLVMCodeGenLevelDefault
     LLVMCodeGenLevelAggressive])

  (defenum LLVMCodeModel
    [LLVMCodeModelDefault
     LLVMCodeModelJITDefault
     LLVMCodeModelSmall
     LLVMCodeModelKernel
     LLVMCodeModelMedium
     LLVMCodeModelLarge])


  (defenum LLVMLinkage
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

  (defenum LLVMIntPredicate
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

  (defenum LLVMRealPredicate
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

  (defenum LLVMOpcode
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

  (defenum LLVMAtomicOrdering
    [LLVMAtomicOrderingNotAtomic
    LLVMAtomicOrderingUnordered
    LLVMAtomicOrderingMonotonic
    __
    LLVMAtomicOrderingAcquire
    LLVMAtomicOrderingRelease
    LLVMAtomicOrderingAcquireRelease
    LLVMAtomicOrderingSequentiallyConsistent])

  (defenum LLVMAtomicRMWBinOp
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
