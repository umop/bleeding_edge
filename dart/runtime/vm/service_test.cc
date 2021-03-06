// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#include "include/dart_debugger_api.h"
#include "vm/dart_api_impl.h"
#include "vm/dart_entry.h"
#include "vm/debugger.h"
#include "vm/globals.h"
#include "vm/message_handler.h"
#include "vm/os.h"
#include "vm/port.h"
#include "vm/service.h"
#include "vm/unit_test.h"

namespace dart {

class ServiceTestMessageHandler : public MessageHandler {
 public:
  ServiceTestMessageHandler() : _msg(NULL) {}

  ~ServiceTestMessageHandler() {
    free(_msg);
  }

  bool HandleMessage(Message* message) {
    if (_msg != NULL) {
      free(_msg);
    }

    // Parse the message.
    SnapshotReader reader(message->data(), message->len(),
                          Snapshot::kMessage, Isolate::Current());
    const Object& response_obj = Object::Handle(reader.ReadObject());
    String& response = String::Handle();
    response ^= response_obj.raw();
    _msg = strdup(response.ToCString());
    return true;
  }

  const char* msg() const { return _msg; }

 private:
  char* _msg;
};


static RawInstance* Eval(Dart_Handle lib, const char* expr) {
  Dart_Handle result = Dart_EvaluateExpr(lib, NewString(expr));
  EXPECT_VALID(result);
  Isolate* isolate = Isolate::Current();
  const Instance& instance = Api::UnwrapInstanceHandle(isolate, result);
  return instance.raw();
}


static RawInstance* EvalF(Dart_Handle lib, const char* fmt, ...) {
  Isolate* isolate = Isolate::Current();

  va_list args;
  va_start(args, fmt);
  intptr_t len = OS::VSNPrint(NULL, 0, fmt, args);
  va_end(args);

  char* buffer = isolate->current_zone()->Alloc<char>(len + 1);
  va_list args2;
  va_start(args2, fmt);
  OS::VSNPrint(buffer, (len + 1), fmt, args2);
  va_end(args2);

  return Eval(lib, buffer);
}


/*
static RawFunction* GetFunction(const Class& cls, const char* name) {
  const Function& result = Function::Handle(cls.LookupDynamicFunction(
      String::Handle(String::New(name))));
  EXPECT(!result.IsNull());
  return result.raw();
}


static RawFunction* GetStaticFunction(const Class& cls, const char* name) {
  const Function& result = Function::Handle(cls.LookupStaticFunction(
      String::Handle(String::New(name))));
  EXPECT(!result.IsNull());
  return result.raw();
}


static RawField* GetField(const Class& cls, const char* name) {
  const Field& field =
      Field::Handle(cls.LookupField(String::Handle(String::New(name))));
  EXPECT(!field.IsNull());
  return field.raw();
}
*/


static RawClass* GetClass(const Library& lib, const char* name) {
  const Class& cls = Class::Handle(
      lib.LookupClass(String::Handle(Symbols::New(name))));
  EXPECT(!cls.IsNull());  // No ambiguity error expected.
  return cls.raw();
}


TEST_CASE(Service_DebugBreakpoints) {
  const char* kScript =
      "var port;\n"  // Set to our mock port by C++.
      "\n"
      "main() {\n"   // We set breakpoint here.
      "}";

  Isolate* isolate = Isolate::Current();
  Dart_Handle lib = TestCase::LoadTestScript(kScript, NULL);
  EXPECT_VALID(lib);

  // Build a mock message handler and wrap it in a dart port.
  ServiceTestMessageHandler handler;
  Dart_Port port_id = PortMap::CreatePort(&handler);
  Dart_Handle port =
      Api::NewHandle(isolate, DartLibraryCalls::NewSendPort(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(lib, NewString("port"), port));

  Instance& service_msg = Instance::Handle();

  // Add a breakpoint.
  const String& url = String::Handle(String::New(TestCase::url()));
  isolate->debugger()->SetBreakpointAtLine(url, 3);

  // Get the breakpoint list.
  service_msg = Eval(lib, "[port, ['debug', 'breakpoints'], [], []]");
  Service::HandleServiceMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ(
      "{\"type\":\"BreakpointList\",\"breakpoints\":[{"
          "\"type\":\"Breakpoint\",\"id\":1,\"enabled\":true,"
          "\"resolved\":false,"
          "\"location\":{\"type\":\"Location\","
                        "\"script\":\"dart:test-lib\",\"tokenPos\":5}}]}",
      handler.msg());

  // Individual breakpoint.
  service_msg = Eval(lib, "[port, ['debug', 'breakpoints', '1'], [], []]");
  Service::HandleServiceMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ(
      "{\"type\":\"Breakpoint\",\"id\":1,\"enabled\":true,"
       "\"resolved\":false,"
       "\"location\":{\"type\":\"Location\","
                     "\"script\":\"dart:test-lib\",\"tokenPos\":5}}",
      handler.msg());

  // Missing sub-command.
  service_msg = Eval(lib, "[port, ['debug'], [], []]");
  Service::HandleServiceMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ(
      "{\"type\":\"Error\","
       "\"text\":\"Must specify a subcommand\","
       "\"message\":{\"arguments\":[\"debug\"],\"option_keys\":[],"
                    "\"option_values\":[]}}",
      handler.msg());

  // Unrecognized breakpoint.
  service_msg = Eval(lib, "[port, ['debug', 'breakpoints', '1111'], [], []]");
  Service::HandleServiceMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ("{\"type\":\"Error\","
                "\"text\":\"Unrecognized breakpoint id 1111\","
                "\"message\":{"
                    "\"arguments\":[\"debug\",\"breakpoints\",\"1111\"],"
                    "\"option_keys\":[],\"option_values\":[]}}",
               handler.msg());

  // Command too long.
  service_msg =
      Eval(lib, "[port, ['debug', 'breakpoints', '1111', 'green'], [], []]");
  Service::HandleServiceMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ("{\"type\":\"Error\",\"text\":\"Command too long\","
                "\"message\":{\"arguments\":[\"debug\",\"breakpoints\","
                                            "\"1111\",\"green\"],"
                             "\"option_keys\":[],\"option_values\":[]}}",
               handler.msg());

  // Unrecognized subcommand.
  service_msg = Eval(lib, "[port, ['debug', 'nosferatu'], [], []]");
  Service::HandleServiceMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ("{\"type\":\"Error\","
                "\"text\":\"Unrecognized subcommand 'nosferatu'\","
                "\"message\":{\"arguments\":[\"debug\",\"nosferatu\"],"
                             "\"option_keys\":[],\"option_values\":[]}}",
               handler.msg());
}


TEST_CASE(Service_Classes) {
  const char* kScript =
      "var port;\n"  // Set to our mock port by C++.
      "\n"
      "class A {\n"
      "  var a;\n"
      "  dynamic b() {}\n"
      "  dynamic c() {\n"
      "    var d = () { b(); };\n"
      "    return d;\n"
      "  }\n"
      "}\n"
      "main() {\n"
      "  var z = new A();\n"
      "  var x = z.c();\n"
      "  x();\n"
      "}";

  Isolate* isolate = Isolate::Current();
  Dart_Handle h_lib = TestCase::LoadTestScript(kScript, NULL);
  EXPECT_VALID(h_lib);
  Library& lib = Library::Handle();
  lib ^= Api::UnwrapHandle(h_lib);
  EXPECT(!lib.IsNull());
  Dart_Handle result = Dart_Invoke(h_lib, NewString("main"), 0, NULL);
  EXPECT_VALID(result);
  const Class& class_a = Class::Handle(GetClass(lib, "A"));
  EXPECT(!class_a.IsNull());
  intptr_t cid = class_a.id();

  // Build a mock message handler and wrap it in a dart port.
  ServiceTestMessageHandler handler;
  Dart_Port port_id = PortMap::CreatePort(&handler);
  Dart_Handle port =
      Api::NewHandle(isolate, DartLibraryCalls::NewSendPort(port_id));
  EXPECT_VALID(port);
  EXPECT_VALID(Dart_SetField(h_lib, NewString("port"), port));

  Instance& service_msg = Instance::Handle();

  // Request an invalid class id.
  service_msg = Eval(h_lib, "[port, ['classes', '999999'], [], []]");
  Service::HandleServiceMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ(
    "{\"type\":\"Error\",\"text\":\"999999 is not a valid class id.\","
    "\"message\":{\"arguments\":[\"classes\",\"999999\"],"
    "\"option_keys\":[],\"option_values\":[]}}", handler.msg());

  // Request the class A over the service.
  service_msg = EvalF(h_lib, "[port, ['classes', '%" Pd "'], [], []]", cid);
  Service::HandleServiceMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ(
      "{\"type\":\"Class\",\"id\":\"classes\\/1009\",\"name\":\"A\","
      "\"user_name\":\"A\",\"implemented\":false,\"abstract\":false,"
      "\"patch\":false,\"finalized\":true,\"const\":false,\"super\":"
      "{\"type\":\"@Class\",\"id\":\"classes\\/35\",\"name\":\"Object\","
      "\"user_name\":\"Object\"},\"library\":{\"type\":\"@Library\",\"id\":"
      "\"libraries\\/12\",\"name\":\"\",\"user_name\":\"dart:test-lib\"},"
      "\"fields\":[{\"type\":\"@Field\",\"id\":\"classes\\/1009\\/fields\\/0\","
      "\"name\":\"a\",\"user_name\":\"a\",\"owner\":{\"type\":\"@Class\","
      "\"id\":\"classes\\/1009\",\"name\":\"A\",\"user_name\":\"A\"},"
      "\"declared_type\":{\"type\":\"@Class\",\"id\":\"classes\\/106\","
      "\"name\":\"dynamic\",\"user_name\":\"dynamic\"},\"static\":false,"
      "\"final\":false,\"const\":false}],\"functions\":["
      "{\"type\":\"@Function\",\"id\":\"classes\\/1009\\/functions\\/0\","
      "\"name\":\"get:a\",\"user_name\":\"A.a\"},{\"type\":\"@Function\","
      "\"id\":\"classes\\/1009\\/functions\\/1\",\"name\":\"set:a\","
      "\"user_name\":\"A.a=\"},{\"type\":\"@Function\",\"id\":"
      "\"classes\\/1009\\/functions\\/2\",\"name\":\"b\",\"user_name\":\"A.b\"}"
      ",{\"type\":\"@Function\",\"id\":\"classes\\/1009\\/functions\\/3\","
      "\"name\":\"c\",\"user_name\":\"A.c\"},{\"type\":\"@Function\",\"id\":"
      "\"classes\\/1009\\/functions\\/4\",\"name\":\"A.\",\"user_name\":"
      "\"A.A\"}]}",
      handler.msg());

  // Request function 0 from class A.
  service_msg = EvalF(h_lib, "[port, ['classes', '%" Pd "', 'functions', '0'],"
                              "[], []]", cid);
  Service::HandleServiceMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ(
    "{\"type\":\"Function\",\"id\":\"classes\\/1009\\/functions\\/0\",\"name\":"
    "\"get:a\",\"user_name\":\"A.a\",\"is_static\":false,\"is_const\":false,"
    "\"is_optimizable\":true,\"is_inlinable\":false,\"kind\":"
    "\"implicit getter\",\"unoptimized_code\":{\"type\":\"null\"},"
    "\"usage_counter\":0,\"optimized_call_site_count\":0,\"code\":"
    "{\"type\":\"null\"},\"deoptimizations\":0}", handler.msg());

  // Request field 0 from class A.
  service_msg = EvalF(h_lib, "[port, ['classes', '%" Pd "', 'fields', '0'],"
                              "[], []]", cid);
  Service::HandleServiceMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ(
    "{\"type\":\"Field\",\"id\":\"classes\\/1009\\/fields\\/0\",\"name\":\"a\","
    "\"user_name\":\"a\",\"owner\":{\"type\":\"@Class\",\"id\":"
    "\"classes\\/1009\",\"name\":\"A\",\"user_name\":\"A\"},\"declared_type\":"
    "{\"type\":\"@Class\",\"id\":\"classes\\/106\",\"name\":\"dynamic\","
    "\"user_name\":\"dynamic\"},\"static\":false,\"final\":false,\"const\":"
    "false,\"guard_nullable\":true,\"guard_class\":{\"type\":\"@Class\","
    "\"id\":\"classes\\/105\",\"name\":\"Null\",\"user_name\":\"Null\"},"
    "\"guard_length\":\"variable\"}", handler.msg());

  // Invalid sub command.
  service_msg = EvalF(h_lib, "[port, ['classes', '%" Pd "', 'huh', '0'],"
                              "[], []]", cid);
  Service::HandleServiceMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ(
    "{\"type\":\"Error\",\"text\":\"Invalid sub collection huh\",\"message\":"
    "{\"arguments\":[\"classes\",\"1009\",\"huh\",\"0\"],\"option_keys\":[],"
    "\"option_values\":[]}}", handler.msg());

  // Invalid field request.
  service_msg = EvalF(h_lib, "[port, ['classes', '%" Pd "', 'fields', '9'],"
                              "[], []]", cid);
  Service::HandleServiceMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ(
    "{\"type\":\"Error\",\"text\":\"fields id (9) must be in [0, 1).\","
    "\"message\":{\"arguments\":[\"classes\",\"1009\",\"fields\",\"9\"],"
    "\"option_keys\":[],\"option_values\":[]}}", handler.msg());

  // Invalid function request.
  service_msg = EvalF(h_lib, "[port, ['classes', '%" Pd "', 'functions', '9'],"
                              "[], []]", cid);
  Service::HandleServiceMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ(
    "{\"type\":\"Error\",\"text\":\"functions id (9) must be in [0, 5).\","
    "\"message\":{\"arguments\":[\"classes\",\"1009\",\"functions\",\"9\"],"
    "\"option_keys\":[],\"option_values\":[]}}", handler.msg());


  // Invalid field subcommand.
  service_msg = EvalF(h_lib, "[port, ['classes', '%" Pd "', 'fields', '9', 'x']"
                             ",[], []]", cid);
  Service::HandleServiceMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ(
    "{\"type\":\"Error\",\"text\":\"Command too long\",\"message\":"
    "{\"arguments\":[\"classes\",\"1009\",\"fields\",\"9\",\"x\"],"
    "\"option_keys\":[],\"option_values\":[]}}",
    handler.msg());

  // Invalid function request.
  service_msg = EvalF(h_lib, "[port, ['classes', '%" Pd "', 'functions', '9',"
                             "'x'], [], []]", cid);
  Service::HandleServiceMessage(isolate, service_msg);
  handler.HandleNextMessage();
  EXPECT_STREQ(
    "{\"type\":\"Error\",\"text\":\"Command too long\",\"message\":"
    "{\"arguments\":[\"classes\",\"1009\",\"functions\",\"9\",\"x\"],"
    "\"option_keys\":[],\"option_values\":[]}}",
    handler.msg());
}

}  // namespace dart
