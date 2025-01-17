/*
 * Copyright (c) 2021 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 2.0, or
 * GNU General Public License version 2, or
 * GNU Lesser General Public License version 2.1.
 */
package org.truffleruby.language;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.Node;
import org.truffleruby.RubyContext;
import org.truffleruby.RubyLanguage;
import org.truffleruby.language.arguments.CheckKeywordArityNode;
import org.truffleruby.language.arguments.RubyArguments;
import org.truffleruby.language.control.RaiseException;
import org.truffleruby.language.control.ReturnID;
import org.truffleruby.language.methods.Arity;
import org.truffleruby.language.methods.SharedMethodInfo;
import org.truffleruby.language.methods.Split;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;

public abstract class RubyCheckArityRootNode extends RubyRootNode {

    @Child private CheckKeywordArityNode checkKeywordArityNode;

    public final Arity arityForCheck;

    @CompilationFinal private boolean checkArityProfile;

    public RubyCheckArityRootNode(
            RubyLanguage language,
            SourceSection sourceSection,
            FrameDescriptor frameDescriptor,
            SharedMethodInfo sharedMethodInfo,
            RubyNode body,
            Split split,
            ReturnID returnID,
            Arity arityForCheck) {
        super(language, sourceSection, frameDescriptor, sharedMethodInfo, body, split, returnID);

        final boolean acceptsKeywords = arityForCheck.acceptsKeywords();
        this.arityForCheck = arityForCheck;
        this.checkKeywordArityNode = acceptsKeywords ? new CheckKeywordArityNode(arityForCheck) : null;
    }

    protected void checkArity(VirtualFrame frame) {
        if (checkKeywordArityNode == null) {
            int given = RubyArguments.getArgumentsCount(frame);
            if (!arityForCheck.check(given)) {
                if (!checkArityProfile) {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    checkArityProfile = true;
                }

                checkArityError(arityForCheck, given, this);
            }
        } else {
            checkKeywordArityNode.checkArity(frame, arityForCheck);
        }
    }

    public static void checkArityError(Arity arity, int given, Node currentNode) {
        final RubyContext context = RubyContext.get(currentNode);
        if (arity.hasRest()) {
            throw new RaiseException(
                    context,
                    context.getCoreExceptions().argumentErrorPlus(given, arity.getRequired(), currentNode));
        } else if (arity.getOptional() > 0) {
            throw new RaiseException(
                    context,
                    context.getCoreExceptions().argumentError(
                            given,
                            arity.getRequired(),
                            arity.getOptional(),
                            currentNode));
        } else {
            throw new RaiseException(
                    context,
                    context.getCoreExceptions().argumentError(given, arity.getRequired(), currentNode));
        }
    }

}
