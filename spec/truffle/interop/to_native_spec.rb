# Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0, or
# GNU General Public License version 2, or
# GNU Lesser General Public License version 2.1.

require_relative '../../ruby/spec_helper'

describe "Truffle::Interop.to_native" do

  it "is not supported for nil" do
    -> { Truffle::Interop.to_native(nil) }.should_not raise_error
    Truffle::Interop.pointer?(nil).should be_false
  end

  it "is not supported for objects which cannot be converted to a pointer" do
    object = Object.new
    -> { Truffle::Interop.to_native(object) }.should_not raise_error(ArgumentError)
    Truffle::Interop.pointer?(object).should be_false
  end

  it "calls #to_native does internal conversion to support as_pointer" do
    obj = Object.new

    def obj.to_native
      @pointer = Truffle::FFI::Pointer.new(0x123)
    end

    def obj.__pointer__?
      !@pointer.nil?
    end

    def obj.__address__
      @pointer.__address__
    end

    Truffle::Interop.pointer?(obj).should be_false
    Truffle::Interop.to_native(obj)
    Truffle::Interop.pointer?(obj).should be_true
    Truffle::Interop.as_pointer(obj).should == 0x123
  end

end
