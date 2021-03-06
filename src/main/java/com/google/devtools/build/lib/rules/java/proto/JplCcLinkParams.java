// Copyright 2017 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.rules.java.proto;

import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.analysis.RuleContext;
import com.google.devtools.build.lib.analysis.TransitiveInfoCollection;
import com.google.devtools.build.lib.analysis.configuredtargets.RuleConfiguredTarget;
import com.google.devtools.build.lib.rules.cpp.AbstractCcLinkParamsStore;
import com.google.devtools.build.lib.rules.cpp.CcLinkParams;
import com.google.devtools.build.lib.rules.cpp.CcLinkParamsStore;
import com.google.devtools.build.lib.rules.cpp.CcLinkingInfo;
import com.google.devtools.build.lib.rules.java.JavaCcLinkParamsProvider;
import java.util.ArrayList;
import java.util.List;

/**
 * Methods that all java_xxx_proto_library rules use to construct JavaCcLinkParamsProvider's.
 */
public class JplCcLinkParams {

  /**
   * Creates a CcLinkParamsStore based on 'deps' and an explicit list of proto runtimes, in the
   * context of a java_xxx_proto_library and its aspects.
   *
   * @param ruleContext used to extract 'deps'. the 'deps' are expected to provide
   *     JavaProtoLibraryAspectProvider, which is the case when a java_xxx_proto_library rule
   *     depends on proto_library's with the aspect, and when an aspect node depends on its
   *     dependency's aspect node.
   * @param protoRuntimes a list of java_library.
   */
  public static JavaCcLinkParamsProvider createCcLinkParamsStore(
      final RuleContext ruleContext, final ImmutableList<TransitiveInfoCollection> protoRuntimes) {
    List<JavaCcLinkParamsProvider> providers = new ArrayList<>();
    for (TransitiveInfoCollection t :
        ruleContext.getPrerequisites("deps", RuleConfiguredTarget.Mode.TARGET)) {
      providers.add(
          t.getProvider(JavaProtoLibraryAspectProvider.class)
              .getTransitiveInfoProviderMap()
              .getProvider(JavaCcLinkParamsProvider.class));
    }
    CcLinkingInfo.Builder builder = CcLinkingInfo.Builder.create();
    builder.setCcLinkParamsStore(
        new CcLinkParamsStore(
            new AbstractCcLinkParamsStore() {
              @Override
              protected void collect(
                  CcLinkParams.Builder builder, boolean linkingStatically, boolean linkShared) {
                for (JavaCcLinkParamsProvider provider : providers) {
                  builder.add(provider.getCcLinkingInfo().getCcLinkParamsStore());
                }
                builder.addTransitiveTargets(protoRuntimes);
              }
            }));
    return new JavaCcLinkParamsProvider(builder.build());
  }
}
