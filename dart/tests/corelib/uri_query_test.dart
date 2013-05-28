// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import "package:expect/expect.dart";

void testInvalidArguments() {
}

void testEncodeQueryComponent() {
  // This exact data is from posting a form in Chrome 26 with the one
  // exception that * is encoded as %30 and ~ is not encoded as %7E.
  Expect.equals(
      "%21%22%23%24%25%26%27%28%29%2A%2B%2C-.%2F%"
      "3A%3B%3C%3D%3E%3F%40%5B%5C%5D%5E_%60%7B%7C%7D~",
      Uri.encodeQueryComponent("!\"#\$%&'()*+,-./:;<=>?@[\\]^_`{|}~"));
  Expect.equals("+%2B+", Uri.encodeQueryComponent(" + "));
  Expect.equals("%2B+%2B", Uri.encodeQueryComponent("+ +"));
}

void testQueryParameters() {
  test(String query, Map<String, String> parameters) {
    check(uri) {
      Expect.equals(query, uri.query);
      if (query.isEmpty) {
        Expect.equals(query, uri.toString());
      } else {
        Expect.equals("?$query", uri.toString());
      }
      if (parameters.containsValue(null)) {
      } else {
        Expect.mapEquals(parameters, uri.queryParameters);
      }
    }

    var uri1 = new Uri(queryParameters: parameters);
    var uri2 = new Uri(query: query);
    var uri3 = Uri.parse("?$query");
    check(uri1);
    check(uri2);
    check(uri3);
    Expect.equals(uri1, uri3);
    Expect.equals(uri2, uri3);
  }

  test("", {});
  test("A", {"A": null});
  test("A", {"A": ""});
  test("A=a", {"A": "a"});
  test("A=+", {"A": " "});
  test("A=%2B", {"A": "+"});
  test("A=a&B", {"A": "a", "B": null});
  test("A=a&B", {"A": "a", "B": ""});
  test("A=a&B=b", {"A": "a", "B": "b"});

  var unreserved = "-._~0123456789"
                   "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                   "abcdefghijklmnopqrstuvwxyz";
  var encoded = new StringBuffer();
  var unencoded = new StringBuffer();
  for (int i = 32; i < 128; i++) {
    if (i == 32) {
      encoded.write("+");
    } else if (unreserved.indexOf(new String.fromCharCode(i)) != -1) {
      encoded.writeCharCode(i);
    } else {
      encoded.write("%");
      encoded.write(i.toRadixString(16).toUpperCase());
    }
    unencoded.writeCharCode(i);
  }
  encoded = encoded.toString();
  unencoded = unencoded.toString();
  print(encoded);
  print(unencoded);
  test("a=$encoded", {"a": unencoded});
  test("a=$encoded&b=$encoded", {"a": unencoded, "b": unencoded});

  var map = new Map();
  map[unencoded] = unencoded;
  test("$encoded=$encoded", map);
}

testInvalidQueryParameters() {
  test(String query, Map<String, String> parameters) {
    check(uri) {
      Expect.equals(query, uri.query);
      if (query.isEmpty) {
        Expect.equals(query, uri.toString());
      } else {
        Expect.equals("?$query", uri.toString());
      }
      if (parameters.containsValue(null)) {
      } else {
        Expect.mapEquals(parameters, uri.queryParameters);
      }
    }

    var uri1 = new Uri(query: query);
    var uri2 = Uri.parse("?$query");
    check(uri1);
    check(uri2);
    Expect.equals(uri1, uri2);
  }

  test("=", {});
  test("=xxx", {});
  test("===", {});
  test("=xxx=yyy=zzz", {});
  test("=&=&=", {});
  test("=xxx&=yyy&=zzz", {});
  test("&=&=&", {});
  test("&=xxx&=xxx&", {});
}

main() {
  testInvalidArguments();
  testEncodeQueryComponent();
  testQueryParameters();
  testInvalidQueryParameters();
}
