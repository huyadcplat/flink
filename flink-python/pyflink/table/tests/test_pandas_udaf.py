################################################################################
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
# limitations under the License.
################################################################################
import unittest

from pyflink.table.types import DataTypes
from pyflink.table.udf import udaf, udf, AggregateFunction
from pyflink.testing import source_sink_utils
from pyflink.testing.test_case_utils import PyFlinkBlinkBatchTableTestCase


class BatchPandasUDAFITTests(PyFlinkBlinkBatchTableTestCase):
    def test_group_aggregate_function(self):
        t = self.t_env.from_elements(
            [(1, 2, 3), (3, 2, 3), (2, 1, 3), (1, 5, 4), (1, 8, 6), (2, 3, 4)],
            DataTypes.ROW(
                [DataTypes.FIELD("a", DataTypes.TINYINT()),
                 DataTypes.FIELD("b", DataTypes.SMALLINT()),
                 DataTypes.FIELD("c", DataTypes.INT())]))

        table_sink = source_sink_utils.TestAppendSink(
            ['a', 'b', 'c'],
            [DataTypes.TINYINT(), DataTypes.FLOAT(), DataTypes.INT()])
        self.t_env.register_table_sink("Results", table_sink)
        # general udf
        add = udf(lambda a: a + 1, result_type=DataTypes.INT())
        # pandas udf
        substract = udf(lambda a: a - 1, result_type=DataTypes.INT(), func_type="pandas")
        max_udaf = udaf(lambda a: a.max(), result_type=DataTypes.INT(), func_type="pandas")
        t.group_by("a") \
            .select(t.a, mean_udaf(add(t.b)), max_udaf(substract(t.c))) \
            .execute_insert("Results") \
            .wait()
        actual = source_sink_utils.results()
        self.assert_equals(actual, ["1,6.0,5", "2,3.0,3", "3,3.0,2"])

    def test_group_aggregate_without_keys(self):
        t = self.t_env.from_elements(
            [(1, 2, 3), (3, 2, 3), (2, 1, 3), (1, 5, 4), (1, 8, 6), (2, 3, 4)],
            DataTypes.ROW(
                [DataTypes.FIELD("a", DataTypes.TINYINT()),
                 DataTypes.FIELD("b", DataTypes.SMALLINT()),
                 DataTypes.FIELD("c", DataTypes.INT())]))

        table_sink = source_sink_utils.TestAppendSink(
            ['a'],
            [DataTypes.INT()])
        min_add = udaf(lambda a, b, c: a.min() + b.min() + c.min(),
                       result_type=DataTypes.INT(), func_type="pandas")
        self.t_env.register_table_sink("Results", table_sink)
        t.select(min_add(t.a, t.b, t.c)) \
            .execute_insert("Results") \
            .wait()
        actual = source_sink_utils.results()
        self.assert_equals(actual, ["5"])

    def test_group_aggregate_with_aux_group(self):
        t = self.t_env.from_elements(
            [(1, 2, 3), (3, 2, 3), (2, 1, 3), (1, 5, 4), (1, 8, 6), (2, 3, 4)],
            DataTypes.ROW(
                [DataTypes.FIELD("a", DataTypes.TINYINT()),
                 DataTypes.FIELD("b", DataTypes.SMALLINT()),
                 DataTypes.FIELD("c", DataTypes.INT())]))

        table_sink = source_sink_utils.TestAppendSink(
            ['a', 'b', 'c', 'd'],
            [DataTypes.TINYINT(), DataTypes.INT(), DataTypes.FLOAT(), DataTypes.INT()])
        self.t_env.register_table_sink("Results", table_sink)
        self.t_env.get_config().get_configuration().set_string('python.metric.enabled', 'true')
        self.t_env.register_function("max_add", udaf(MaxAdd(),
                                                     result_type=DataTypes.INT(),
                                                     func_type="pandas"))
        self.t_env.create_temporary_system_function("mean_udaf", mean_udaf)
        t.group_by("a") \
            .select("a, a + 1 as b, a + 2 as c") \
            .group_by("a, b") \
            .select("a, b, mean_udaf(b), max_add(b, c, 1)") \
            .execute_insert("Results") \
            .wait()
        actual = source_sink_utils.results()
        self.assert_equals(actual, ["1,2,2.0,6", "2,3,3.0,8", "3,4,4.0,10"])


@udaf(result_type=DataTypes.FLOAT(), func_type="pandas")
def mean_udaf(v):
    return v.mean()


class MaxAdd(AggregateFunction, unittest.TestCase):

    def open(self, function_context):
        mg = function_context.get_metric_group()
        self.counter = mg.add_group("key", "value").counter("my_counter")
        self.counter_sum = 0

    def get_value(self, accumulator):
        # counter
        self.counter.inc(10)
        self.counter_sum += 10
        self.assertEqual(self.counter_sum, self.counter.get_count())
        return accumulator[0]

    def create_accumulator(self):
        return []

    def accumulate(self, accumulator, *args):
        result = 0
        for arg in args:
            result += arg.max()
        accumulator.append(result)


if __name__ == '__main__':
    import unittest

    try:
        import xmlrunner

        testRunner = xmlrunner.XMLTestRunner(output='target/test-reports')
    except ImportError:
        testRunner = None
    unittest.main(testRunner=testRunner, verbosity=2)
