// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import 'dart:mirrors';

main() {
  var name = MirrorSystem.getName(#foo);
  if (name != 'foo') throw 'Wrong name: $name != foo';
}
