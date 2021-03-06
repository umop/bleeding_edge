// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import "package:expect/expect.dart";

// Test for values of some basic types.


class A native "A" {
  returnNull() native;
  returnUndefined() native;
  returnEmptyString() native;
  returnZero() native;
}

A makeA() native;

void setup() native """
function A() {}
A.prototype.returnNull = function() { return null; };
A.prototype.returnUndefined = function() { return void 0; };
A.prototype.returnEmptyString = function() { return ""; };
A.prototype.returnZero = function() { return 0; };
makeA = function(){return new A;};
""";


main() {
  setup();
  A a = makeA();
  Expect.equals(null, a.returnNull());
  Expect.equals(null, a.returnUndefined());

  Expect.equals('', a.returnEmptyString());
  Expect.isTrue(a.returnEmptyString().isEmpty);
  Expect.isTrue(a.returnEmptyString() is String);

  Expect.isTrue(a.returnZero() is int);
  Expect.equals(0, a.returnZero());
}
