; Taken from https://github.com/halgari/mjolnir/blob/5b1c0cf1c34d5521438ee33974715d98abb7884d/src/mjolnir/llvmc.clj
(ns clj-llvm.llvm.interop
  (:import (com.sun.jna Pointer))
  (:require [clj-llvm.native-interop :refer [with-lib
                                             defnative
                                             defenum]]))


(with-lib LLVM-3.4
  (def LLVMCCallConv 0)
  (def LLVMFastCallConv 8)
  (def LLVMColdCallConv 9)
  (def LLVMX86StdcallCallConv 64)
  (def LLVMX86FastcallCallConv 65)

  (defnative Integer LLVMSetFunctionCallConv)
  (defnative Integer LLVMFindFunction)

  (defnative Pointer LLVMAppendBasicBlock)
  (defnative Pointer LLVMCreateBuilder)

  (defnative Pointer LLVMGetParam)

  (defnative Integer LLVMLinkInJIT)
  (defnative Pointer LLVMGetDefaultTargetTriple)

  (defnative Pointer LLVMModuleCreateWithName)

  (defnative Pointer LLVMInt32Type)
  (defnative Pointer LLVMFunctionType)

  (defnative Pointer LLVMAddFunction)

  (defnative Integer LLVMPositionBuilderAtEnd)

  (defnative Boolean LLVMVerifyModule)

  (def LLVMAbortProcessAction 0)
  (def LLVMPrintMessageAction 1)
  (def LLVMReturnStatusAction 2)

  (defnative Pointer LLVMCreateModuleProviderForExistingModule)

  (defnative Integer LLVMDisposeMessage)
  (defnative Integer LLVMCreateJITCompiler)
  (defnative Integer LLVMCreateInterpreterForModule)
  (defnative Pointer LLVMCreatePassManager)
  (defnative Pointer LLVMGetExecutionEngineTargetData)
  (defnative Integer LLVMAddTargetData)
  (defnative Integer LLVMRunPassManager)
  (defnative Integer LLVMDumpModule)
  (defnative Pointer LLVMPrintModuleToString)
  (defnative Integer LLVMDisposePassManager)
  (defnative Integer LLVMDisposeExecutionEngine)
  (defnative Integer LLVMBuildRet)
  (defnative Integer LLVMBuildRetVoid)

  (defnative Integer LLVMLinkInJIT)
  (defnative Integer LLVMLinkInInterpreter)
  (defnative Integer LLVMInitializeX86Target)
  (defnative Integer LLVMInitializeX86TargetInfo)
  (defnative Integer LLVMInitializeX86TargetMC)
  (defnative Pointer LLVMRunFunction)
  (defnative Boolean LLVMFindFunction)
  (defnative Pointer LLVMCreateGenericValueOfInt)
  (defnative Integer LLVMGenericValueToInt)
  (defnative Pointer LLVMBuildAdd)
  (defnative Pointer LLVMBuildSub)
  (defnative Pointer LLVMConstInt)
  (defnative Pointer LLVMConstReal)
  (defnative Pointer LLVMBuildICmp)
  (defnative Pointer LLVMBuildFCmp)
  (defnative Pointer LLVMIntType)
  (defnative Pointer LLVMVoidType)

  (defnative Pointer LLVMBuildCondBr)
  (defnative Pointer LLVMBuildPhi)
  (defnative Integer LLVMAddIncoming)
  (defnative Pointer LLVMTypeOf)
  (defnative Integer LLVMCountParamTypes)
  (defnative Integer LLVMGetTypeKind)
  (defnative Integer LLVMIsConstant)
  (defnative Integer LLVMDisposeGenericValue)
  (defnative Integer LLVMDisposeBuilder)
  (defnative Pointer LLVMBuildBr)
  (defnative Pointer LLVMBuildCall)
  (defnative Pointer LLVMBuildAlloca)
  (defnative Pointer LLVMBuildFree)
  (defnative Pointer LLVMBuildLoad)
  (defnative Pointer LLVMBuildStore)
  (defnative Pointer LLVMBuildArrayMalloc)
  (defnative Pointer LLVMBuildGEP)
  (defnative Pointer LLVMBuildInBoundsGEP)
  (defnative Pointer LLVMBuildBitCast)
  (defnative Pointer LLVMBuildCast)
  (defnative Pointer LLVMConstString)
  (defnative Pointer LLVMConstInt)
  (defnative Integer LLVMCountStructElementTypes)
  (defnative Pointer LLVMConstPointerCast)
  (defnative Pointer LLVMGetStructElementTypes)
  (defnative Integer LLVMGetTypeKind)
  (defnative Pointer LLVMConstPointerNull)
  (defnative Pointer LLVMInt64Type)
  (defnative Pointer LLVMStructType)
  (defnative Pointer LLVMArrayType)
  (defnative Pointer LLVMVectorType)
  (defnative Pointer LLVMDumpValue)
  (defnative Integer LLVMGetArrayLength)
  (defnative Pointer LLVMGetElementType)
  (defnative Pointer LLVMConstArray)
  (defnative Pointer LLVMConstString)
  (defnative Pointer LLVMConstStruct)
  (defnative Pointer LLVMConstGEP)
  (defnative Pointer LLVMConstVector)
  (defnative Pointer LLVMConstBitCast)
  (defnative Pointer LLVMConstTrunc)
  (defnative Pointer LLVMConstZExt)
  (defnative Integer LLVMCountParams)
  (defnative Pointer LLVMAddGlobal)
  (defnative Pointer LLVMAddGlobalInAddressSpace)
  (defnative Integer LLVMSetInitializer)
  (defnative Integer LLVMWriteBitcodeToFile)
  (defnative Pointer LLVMGetNamedGlobal)
  (defnative Pointer LLVMGetNamedFunction)
  (defnative Pointer LLVMInt8Type)
  (defnative Pointer LLVMInt1Type)
  (defnative Pointer LLVMFloatType)
  (defnative Pointer LLVMDoubleType)
  (defnative Pointer LLVMPointerType)
  (defnative Integer LLVMSetLinkage)
  (defnative Integer LLVMGetIntTypeWidth)
  (defnative Pointer LLVMBuildStructGEP)
  (defnative Pointer LLVMBuildAdd)
  (defnative Pointer LLVMBuildFAdd)
  (defnative Pointer LLVMBuildFSub)
  (defnative Pointer LLVMBuildMul)
  (defnative Pointer LLVMBuildFMul)
  (defnative Pointer LLVMBuildFDiv)
  (defnative Pointer LLVMBuildSub)
  (defnative Pointer LLVMBuildShl)
  (defnative Pointer LLVMBuildLShr)
  (defnative Pointer LLVMBuildAnd)
  (defnative Pointer LLVMBuildNot)
  (defnative Pointer LLVMBuildZExt)
  (defnative Pointer LLVMBuildZExtOrBitCast)
  (defnative Pointer LLVMBuildSExt)
  (defnative Pointer LLVMBuildSExtOrBitCast)
  (defnative Pointer LLVMBuildTrunc)
  (defnative Pointer LLVMBuildTruncOrBitCast)
  (defnative Pointer LLVMBuildFPToSI)
  (defnative Pointer LLVMBuildSIToFP)
  (defnative Pointer LLVMBuildIntCast)
  (defnative Pointer LLVMBuildOr)
  (defnative Pointer LLVMBuildMalloc)
  (defnative Pointer LLVMSizeOf)
  (defnative Pointer LLVMConstNull)
  (defnative Pointer LLVMBuildBinOp)
  (defnative Pointer LLVMBuildAtomicRMW)

  (defnative Pointer LLVMBuildExtractElement)
  (defnative Pointer LLVMBuildInsertElement)

  (defnative Integer LLVMAddConstantPropagationPass)
  (defnative Integer LLVMAddInstructionCombiningPass)
  (defnative Integer LLVMAddPromoteMemoryToRegisterPass)
  (defnative Integer LLVMAddGVNPass)
  (defnative Integer LLVMAddCFGSimplificationPass)
  (defnative Integer LLVMAddBBVectorizePass)
  (defnative Integer LLVMAddLoopVectorizePass)
  (defnative Integer LLVMAddLoopUnrollPass)
  (defnative Pointer LLVMAddFunctionInliningPass)



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
    0
    [LLVMAtomicOrderingNotAtomic
    LLVMAtomicOrderingUnordered
    LLVMAtomicOrderingMonotonic
    __
    LLVMAtomicOrderingAcquire
    LLVMAtomicOrderingRelease
    LLVMAtomicOrderingAcquireRelease
    LLVMAtomicOrderingSequentiallyConsistent])

  (defenum LLVMAtomicRMWBinOp
    0
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
     LLVMAtomicRMWBinOpUMin])

  (defnative Integer LLVMInitializeX86AsmPrinter)
  (defnative Integer LLVMInitializeX86AsmParser)

  (def CCallConv 0)
  (def FastCallConv 8)
  (def ColdCallConv 9)
  (def X86StdcallCallConv 64)
  (def X86FastcallCallConv 65)
  (def PTXGlobal 71)
  (def PTXDevice 72)

  (def AbortProcessAction 0)
  (def PrintMessageAction 1)
  (def ReturnStatusAction 2)







  (defnative Pointer LLVMGetFirstTarget)
  (defnative Pointer LLVMGetNextTarget)
  (defnative String LLVMGetTargetName)
  (defnative String LLVMGetTargetDescription)
  (defnative Boolean LLVMTargetHasJIT)
  (defnative Boolean LLVMTargetHasTargetMachine)
  (defnative Boolean LLVMTargetHasAsmBackend)
  (defnative String LLVMGetTarget)
  (defnative Pointer LLVMCreateTargetMachine)
  (defnative Boolean LLVMTargetMachineEmitToFile)
  (defnative Pointer LLVMGetTargetMachineData)
  (defnative Pointer LLVMSetDataLayout)
  (defnative Integer LLVMSetTarget)
  (defnative Pointer LLVMCreateTargetData)
  (defnative String LLVMGetTargetMachineTriple))



(defn target-info [t]
  {:target t
   :name (LLVMGetTargetName t)
   :desc (LLVMGetTargetDescription t)
   :jit? (LLVMTargetHasJIT t)
   :machine? (LLVMTargetHasTargetMachine t)
   :asm? (LLVMTargetHasAsmBackend t)})

(defn target-seq
  ([]
     (let [ft (LLVMGetFirstTarget)]
       (when ft
         (cons (target-info ft)
               (lazy-seq
                (target-seq ft))))))
  ([t]
     (let [nt (LLVMGetNextTarget t)]
       (when nt
         (cons (target-info nt)
               (lazy-seq
                (target-seq nt)))))))
