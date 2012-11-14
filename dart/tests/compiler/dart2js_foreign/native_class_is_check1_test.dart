// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Test for correct simple is-checks on hidden native classes.

import 'native_metadata.dart';

interface I {
  I read();
  write(I x);
}

// Native implementation.

@Native("*A")
class A implements I  {
  // The native class accepts only other native instances.
  @native A read();
  @native write(A x);
}

@native makeA();

@Native("""
// This code is all inside 'setup' and so not accesible from the global scope.
function A(){}
A.prototype.read = function() { return this._x; };
A.prototype.write = function(x) { this._x = x; };
makeA = function(){return new A};
""")
void setup();

class B {}

main() {
  setup();

  var a1 = makeA();
  var ob = new Object();

  Expect.isFalse(ob is I);
  Expect.isFalse(ob is A);
  Expect.isFalse(ob is B);

  Expect.isTrue(a1 is I);
  Expect.isTrue(a1 is A);
  Expect.isTrue(a1 is !B);
}
