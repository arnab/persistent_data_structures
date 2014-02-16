// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.jruby.ext.persistent;

import java.lang.Override;
import java.lang.Thread;
import java.lang.reflect.Field;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.jruby.*;
import org.jruby.Ruby;
import org.jruby.RubyArgsFile;
import org.jruby.javasupport.JavaUtil;
import org.jruby.RubyArray;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;
import java.util.concurrent.atomic.AtomicReference;

public class PersistentVectorLibrary implements Library {
    static public RubyClass Node;
    static public RubyClass PersistentVector;

    public void load(Ruby runtime, boolean wrap) {
        RubyModule persistent = runtime.getOrCreateModule("Persistent");
        RubyClass persistentVector = persistent.defineOrGetClassUnder("Vector", runtime.getObject());
        persistentVector.setAllocator(new ObjectAllocator() {
            @Override
            public IRubyObject allocate(Ruby ruby, RubyClass rubyClass) {
                return new PersistentVector(ruby, rubyClass);
            }
        });
        persistentVector.defineAnnotatedMethods(PersistentVector.class);
    }

    public static class Node extends RubyObject {
        transient public AtomicReference<Thread> edit;
        public IRubyObject array;

        public Node(Ruby runtime, RubyClass rubyClass) {
            super(runtime, rubyClass);
        }

        public Node initialize_params(ThreadContext context) {
            this.array = RubyArray.newArray(context.runtime, 32);
            return this;
        }
    }

    @JRubyClass(name="Vector")
    public static class PersistentVector extends RubyObject {

        int cnt;
        public IRubyObject shift;
        public  IRubyObject root;
        public  IRubyObject tail;


       public PersistentVector(Ruby runtime, RubyClass rubyClass) {
            super(runtime, rubyClass);
        }

        public IRubyObject initialize(ThreadContext context, int cnt, IRubyObject shift, IRubyObject root, IRubyObject tail) {
            this.cnt = cnt;
            this.shift = shift;
            this.root = root;
            this.tail = tail;
            return this;
        }

        @JRubyMethod(name = "vector", meta = true)
        static public IRubyObject vector(ThreadContext context, IRubyObject cls, IRubyObject items) {
            RubyArray arry = RubyArray.newArray(context.runtime);
            PersistentVector ret = new PersistentVector(context.runtime, (RubyClass) cls);
            PersistentVector ret1 = (PersistentVector) ret.initialize(context, 3, RubyFixnum.newFixnum(context.runtime, 4), new Node(context.runtime, Node).initialize(context), arry);
            for(Object item : (RubyArray) items) {
                ret1 = (PersistentVector) ret1.add(context, JavaUtil.convertJavaToRuby(context.runtime, item));
            }
            return ret1;
       }

       @JRubyMethod(name = "tail")
       public IRubyObject tail() {
           return this.tail;
       }

       @JRubyMethod(name = "add", required = 1)
       public IRubyObject add(ThreadContext context, IRubyObject val) {
           PersistentVector ret = new PersistentVector(context.runtime, getMetaClass());
           RubyArray newTail =  (RubyArray) tail.dup();
           newTail.add(val);
           return ret.initialize(context, this.cnt, this.shift, this.root, newTail);
       }

        final int tailoff(){
            if (cnt < 32)
                return 0;
            return ((cnt-1) >>> 5) << 5;
        }


    }

}