// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library value_future_test;

import 'dart:async';

import 'package:scheduled_test/src/value_future.dart';
import 'package:unittest/unittest.dart';

void main() {
  group('works like a normal Future for', () {
    var completer;
    var future;

    setUp(() {
      completer = new Completer();
      future = new ValueFuture(completer.future);
    });

    test('.asStream on success', () {
      expect(future.asStream().toList(), completion(equals(['success'])));
      completer.complete('success');
    });

    test('.asStream on error', () {
      expect(future.asStream().toList(), throwsA(equals('error')));
      completer.completeError('error');
    });

    test('.then and .catchError on success', () {
      expect(future.then((v) => "transformed $v")
              .catchError((error) => "caught ${error}"),
          completion(equals('transformed success')));
      completer.complete('success');
    });

    test('.then and .catchError on error', () {
      expect(future.then((v) => "transformed $v")
              .catchError((error) => "caught ${error}"),
          completion(equals('caught error')));
      completer.completeError('error');
    });

    test('.then with onError on success', () {
      expect(future.then((v) => "transformed $v",
              onError: (error) => "caught ${error}"),
          completion(equals('transformed success')));
      completer.complete('success');
    });

    test('.then with onError on error', () {
      expect(future.then((v) => "transformed $v",
              onError: (error) => "caught ${error}"),
          completion(equals('caught error')));
      completer.completeError('error');
    });

    test('.whenComplete on success', () {
      expect(future.whenComplete(() {
        throw 'whenComplete';
      }), throwsA(equals('whenComplete')));
      completer.complete('success');
    });

    test('.whenComplete on error', () {
      expect(future.whenComplete(() {
        throw 'whenComplete';
      }), throwsA(equals('whenComplete')));
      completer.completeError('error');
    });
  });

  group('before completion', () {
    var future;

    setUp(() {
      future = new ValueFuture(new Completer().future);
    });

    test('.value is null', () {
      expect(future.value, isNull);
    });

    test('.hasValue is false', () {
      expect(future.hasValue, isFalse);
    });
  });

  group('after successful completion', () {
    var future;

    setUp(() {
      var completer = new Completer();
      future = new ValueFuture(completer.future);
      completer.complete(12);
    });

    test('.value is the result of the future', () {
      // The completer calls its listeners asynchronously. We have to wait
      // before we can access the value of the ValueFuture.
      expect(future.then((_) => future.value), completion(equals(12)));
    });

    test('.hasValue is true', () {
      // The completer calls its listeners asynchronously. We have to wait
      // before we can access the value of the ValueFuture.
      expect(future.then((_) => future.hasValue), completion(isTrue));
    });
  });

  group('after an error completion', () {
    var future;
    // Since the completer completes asynchronously we need to wait before the
    // ValueFuture has a value. The safeFuture will execute its callback when
    // this is the case.
    var safeFuture;

    setUp(() {
      var completer = new Completer();
      future = new ValueFuture(completer.future);
      safeFuture = future.catchError((e) {});
      completer.completeError('bad');
    });

    test('.value is null', () {
      expect(safeFuture.then((_) => future.value), completion(isNull));
    });

    test('.hasValue is false', () {
      expect(safeFuture.then((_) => future.hasValue), completion(isFalse));
    });
  });
}
