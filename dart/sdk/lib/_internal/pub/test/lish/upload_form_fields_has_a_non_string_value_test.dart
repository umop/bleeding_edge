// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import 'dart:convert';

import 'package:scheduled_test/scheduled_test.dart';
import 'package:scheduled_test/scheduled_server.dart';

import '../descriptor.dart' as d;
import '../test_pub.dart';
import 'utils.dart';

main() {
  initConfig();
  setUp(d.validPackage.create);

  integration('upload form fields has a non-string value', () {
    var server = new ScheduledServer();
    d.credentialsFile(server, 'access token').create();
    var pub = startPublish(server);

    confirmPublish(pub);

    var body = {
      'url': 'http://example.com/upload',
      'fields': {'field': 12}
    };
    handleUploadForm(server, body);
    expect(pub.nextErrLine(), completion(equals('Invalid server response:')));
    expect(pub.nextErrLine(), completion(equals(JSON.encode(body))));
    pub.shouldExit(1);
  });
}
