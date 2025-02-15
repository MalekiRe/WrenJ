package com.wrenj.vm.WrenVM;

import com.wrenj.include.WrenConfiguration;
import com.wrenj.include.unimplemented.WrenReallocateFn;
import com.wrenj.vm.WrenCompiler.Compiler;
import com.wrenj.vm.WrenValue.ObjModule;
import com.wrenj.vm.WrenValue.Value;
import com.wrenj.vm.unimplemented.*;

import java.util.ArrayList;
import java.util.Optional;

public class WrenVM {
    //HEADER FILE

    static final int WREN_MAX_TEMP_ROOTS = 8;

    //C FILE
    ObjClass boolClass;
    ObjClass classClass;
    ObjClass fiberClass;
    ObjClass fnClass;
    ObjClass listClass;
    ObjClass mapClass;
    ObjClass nullClass;
    ObjClass numClass;
    ObjClass objectClass;
    ObjClass rangeClass;
    ObjClass stringClass;

    // The fiber that is currently running.
    ObjFiber fiber;

    // The loaded modules. Each key is an ObjString (except for the main module,
    // whose key is null) for the module's name and the value is the ObjModule
    // for the module.
    ObjMap modules;

    // The most recently imported module. More specifically, the module whose
    // code has most recently finished executing.
    //
    // Not treated like a GC root since the module is already in [modules].
    ObjModule lastModule;

    // Memory management data:

    // The number of bytes that are known to be currently allocated. Includes all
    // memory that was proven live after the last GC, as well as any new bytes
    // that were allocated since then. Does *not* include bytes for objects that
    // were freed since the last GC.
    long bytesAllocated;

    // The number of total allocated bytes that will trigger the next GC.
    long nextGC;

    // The first object in the linked list of all currently allocated objects.
    Optional<Obj> first;

    // The "gray" set for the garbage collector. This is the stack of unprocessed
    // objects while a garbage collection pass is in process.
    ArrayList<Obj> gray;
    int grayCount;
    int grayCapacity;

    // The list of temporary roots. This is for temporary or new objects that are
    // not otherwise reachable but should not be collected.
    //
    // They are organized as a stack of pointers stored in this array. This
    // implies that temporary roots need to have stack semantics: only the most
    // recently pushed object can be released.
    Obj[] tempRoots = new Obj[WREN_MAX_TEMP_ROOTS];

    int numTempRoots;

    // Pointer to the first node in the linked list of active handles or NULL if
    // there are none.
    WrenHandle handles;

    // Pointer to the bottom of the range of stack slots available for use from
    // the C API. During a foreign method, this will be in the stack of the fiber
    // that is executing a method.
    //
    // If not in a foreign method, this is initially NULL. If the user requests
    // slots by calling wrenEnsureSlots(), a stack is created and this is
    // initialized.
    Value apiStack;

    WrenConfiguration config;

    // Compiler and debugger data:

    // The compiler that is currently compiling code. This is used so that heap
    // allocated objects used by the compiler can be found if a GC is kicked off
    // in the middle of a compile.
    public Compiler compiler;

    // There is a single global symbol table for all method names on all classes.
    // Method calls are dispatched directly by index in this table.
    SymbolTable methodNames;

    public WrenVM(Optional<WrenConfiguration> optionalConfig) {
        WrenReallocateFn reallocateFn;//TODO = defaultReallocate;
        Object userData = null;

        this.config = optionalConfig.orElseGet(WrenConfiguration::initConfiguration);
        this.grayCount = 0;
        this.grayCapacity = 4;
        this.gray = new ArrayList<>();
        this.nextGC = this.config.initialHeapSize;

        //TODO::wrenSymbolTableInit(&vm->methodNames);
        this.modules = null; //TODO::wrenNewMap(vm);
        initializeCore();
    }

    @Deprecated
    void collectGarbage() {
        //TODO::implement this
        System.exit(-1);
    }

    @Deprecated
    Object wrenReallocate(Object memory, long oldSize, long newSize) {
        //TODO::implement
        System.exit(-1);
        return null;
    }

    @Deprecated
    ObjUpValue captureUpValue(ObjFiber fiber, Value local) {
        // If there are no open upvalues at all, we must need a new one.
        if (fiber.openUpvalues == null)
        {
            fiber.openUpvalues = null; //TODO:: = wrenNewUpvalue(vm, local);
            System.exit(-1);
            return fiber.openUpvalues;
        }

        ObjUpValue prevUpvalue = null;
        ObjUpValue upvalue = fiber.openUpvalues;

        // Walk towards the bottom of the stack until we find a previously existing
        // upvalue or pass where it should be.
        while (upvalue != null /*TODO && upvalue.value > local*/)
        {
            prevUpvalue = upvalue;
            upvalue = upvalue.next;
        }
        System.exit(-1);

        // Found an existing upvalue for this local.
        if (upvalue != null && upvalue.value == local) return upvalue;

        // We've walked past this local on the stack, so there must not be an
        // upvalue for it already. Make a new one and link it in in the right
        // place to keep the list sorted.
        ObjUpValue createdUpvalue = null; //TODO = wrenNewUpvalue(vm, local);
        if (prevUpvalue == null)
        {
            // The new one is the first one in the list.
            fiber.openUpvalues = createdUpvalue;
        }
        else
        {
            prevUpvalue.next = createdUpvalue;
        }

        createdUpvalue.next = upvalue;
        return createdUpvalue;
    }

    // Closes any open upvalues that have been created for stack slots at [last]
    // and above.
    @Deprecated
    void closeUpValues(ObjFiber fiber, Value last) {
        while (fiber.openUpvalues != null /*TODO&& fiber.openUpvalues.value >= last*/)
        {
            System.exit(-1);
            ObjUpValue upvalue = fiber.openUpvalues;

            // Move the value into the upvalue itself and point the upvalue to it.
            //TODO::implement this
            /*
            upvalue->closed = *upvalue->value;
            upvalue->value = &upvalue->closed;

            // Remove it from the open upvalue list.
            fiber->openUpvalues = upvalue->next;
             */
        }
    }

    //TODO::figure out how this fits
    /*WrenForeignMethodFn findForeignMethod(String moduleName, String className, boolean isStatic, String signature) {
        WrenForeignMethodFn method = NULL;

        if (vm->config.bindForeignMethodFn != NULL)
        {
            method = vm->config.bindForeignMethodFn(vm, moduleName, className, isStatic,
                    signature);
        }

        // If the host didn't provide it, see if it's an optional one.
        if (method == NULL)
        {
        #if WREN_OPT_META
                    if (strcmp(moduleName, "meta") == 0)
                    {
                        method = wrenMetaBindForeignMethod(vm, className, isStatic, signature);
                    }
        #endif
        #if WREN_OPT_RANDOM
                    if (strcmp(moduleName, "random") == 0)
                    {
                        method = wrenRandomBindForeignMethod(vm, className, isStatic, signature);
                    }
        #endif
        }

        return method;
    }
    */



    //TODO::fix the parameters
    @Deprecated
    private void wrenGrayObj(Object object) {
        //TODO::implement this
        System.exit(-1);
    }

    @Deprecated
    private void wrenGrayValue(Value value) {
        //TODO::implement this
        System.exit(-1);
    }

    private void wrenGrayBuffer(ValueBuffer buffer) {
        //TODO::implement this
        System.exit(-1);
    }

    @Deprecated
    void blackenClass(ObjClass classObj) {
        // The metaclass.
        wrenGrayObj(classObj.obj.classObj);

        // The superclass.
        wrenGrayObj(classObj.superclass);

        // Method function objects
        for (int i = 0; i < classObj.methods.count; i++) {
            //TODO::implement this
            System.exit(-1);
            /*
            if (classObj.methods.data[i].type == METHOD_BLOCK) {
                wrenGrayObj(classObj.methods.data[i].as.closure);
            }
             */
        }

        wrenGrayObj(classObj.name);

        if(classObj.attributes == null) {
            wrenGrayObj(classObj.attributes);
        }


        // Keep track of how much memory is still in use.
        //TODO::i don't think we need this, but i'ma leave it here as a todo
        /*
        vm->bytesAllocated += sizeof(ObjClass);
        vm->bytesAllocated += classObj->methods.capacity * sizeof(Method);
         */

    }

    @Deprecated
    void blackenClosure(ObjClosure closure) {
        //TODO::implement this
        System.exit(-1);
    }

    @Deprecated
    private void initializeCore() {
        //TODO::implement this
        System.exit(-1);
    }
}
