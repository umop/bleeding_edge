// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library isolate_class_test;

import 'dart:async';
import 'test_helper.dart';
import 'package:expect/expect.dart';

class MethodTest extends VmServiceRequestHelper {
  MethodTest(port, id, functionId) :
      super('http://127.0.0.1:$port/$id/$functionId');
  onRequestCompleted(Map reply) {
    Expect.equals('Function', reply['type']);
    Expect.equals('C.c', reply['user_name']);
    Expect.equals(false, reply['is_static']);
  }
}

class FunctionTest extends VmServiceRequestHelper {
  FunctionTest(port, id, functionId) :
      super('http://127.0.0.1:$port/$id/$functionId');

  onRequestCompleted(Map reply) {
    Expect.equals('Function', reply['type']);
    Expect.equals('a', reply['name']);
    Expect.equals(true, reply['is_static']);
  }
}

class StackTraceTest extends VmServiceRequestHelper {
  StackTraceTest(port, id) :
      super('http://127.0.0.1:$port/$id/stacktrace');

  String _aId;
  String _cId;
  onRequestCompleted(Map reply) {
    Expect.equals('StackTrace', reply['type']);
    List members = reply['members'];
    Expect.equals('a', members[0]['name']);
    _aId = members[0]['function']['id'];
    Expect.equals('c', members[2]['name']);
    _cId = members[2]['function']['id'];
  }
}

class IsolateListTest extends VmServiceRequestHelper {
  IsolateListTest(port) : super('http://127.0.0.1:$port/isolates');

  String _isolateId;
  onRequestCompleted(Map reply) {
    IsolateListTester tester = new IsolateListTester(reply);
    tester.checkIsolateCount(2);
    tester.checkIsolateNameContains('isolate_stacktrace_command_script.dart');
    _isolateId = tester.checkIsolateNameContains('myIsolateName');
  }
}

main() {
  var process = new TestLauncher('isolate_stacktrace_command_script.dart');
  process.launch().then((port) {
    var test = new IsolateListTest(port);
    test.makeRequest().then((_) {
      var stackTraceTest =
          new StackTraceTest(port, test._isolateId);
      stackTraceTest.makeRequest().then((_) {
        var functionTest = new FunctionTest(port, test._isolateId,
                                            stackTraceTest._aId);
        functionTest.makeRequest().then((_) {
          var methodTest = new MethodTest(port, test._isolateId,
                                          stackTraceTest._cId);
          methodTest.makeRequest().then((_) {
            process.requestExit();
          });
        });
      });
    });
  });
}
