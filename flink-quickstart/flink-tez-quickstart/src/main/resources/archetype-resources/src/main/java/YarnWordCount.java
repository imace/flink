package ${package};

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

		import org.apache.flink.api.java.DataSet;
		import org.apache.flink.tez.client.RemoteTezEnvironment;
		import org.apache.flink.api.common.functions.FlatMapFunction;
		import org.apache.flink.api.java.tuple.Tuple2;
		import org.apache.flink.util.Collector;

/**
 * Implements the "WordCount" program that computes a simple word occurrence histogram
 * over some sample data
 *
 * <p>
 * This example shows how to:
 * <ul>
 * <li>write a simple Flink program.
 * <li>use Tuple data types.
 * <li>write and use user-defined functions.
 * </ul>
 *
 */
public class YarnWordCount {

	//
	//	Program
	//

	public static void main(String[] args) throws Exception {

		if(!parseParameters(args)) {
			return;
		}
		
		// set up the execution environment
		final RemoteTezEnvironment env = RemoteTezEnvironment.create();
		env.setDegreeOfParallelism(parallelism);

		// get input data
		DataSet<String> text = env.readTextFile(textPath);

		DataSet<Tuple2<String, Integer>> counts =
				// split up the lines in pairs (2-tuples) containing: (word,1)
				text.flatMap(new LineSplitter())
						// group by the tuple field "0" and sum up tuple field "1"
						.groupBy(0)
						.sum(1);

		// emit result
		counts.writeAsCsv(outputPath, "\n", " ");

		// execute program
		env.registerMainClass (YarnWordCount.class);
		env.execute("WordCount Example");
	}

	//
	// 	User Functions
	//

	/**
	 * Implements the string tokenizer that splits sentences into words as a user-defined
	 * FlatMapFunction. The function takes a line (String) and splits it into
	 * multiple pairs in the form of "(word,1)" (Tuple2<String, Integer>).
	 */
	public static final class LineSplitter implements FlatMapFunction<String, Tuple2<String, Integer>> {

		@Override
		public void flatMap(String value, Collector<Tuple2<String, Integer>> out) {
			// normalize and split the line
			String[] tokens = value.toLowerCase().split("\\W+");

			// emit the pairs
			for (String token : tokens) {
				if (token.length() > 0) {
					out.collect(new Tuple2<String, Integer>(token, 1));
				}
			}
		}
	}

	private static boolean fileOutput = false;
	private static String textPath;
	private static String outputPath;
	private static int parallelism;

	private static boolean parseParameters(String[] args) {

		if(args.length > 0) {
			// parse input arguments
			fileOutput = true;
			if(args.length == 3) {
				textPath = args[0];
				outputPath = args[1];
				parallelism = Integer.parseInt(args[2]);
			} else {
				System.err.println("Usage: YarnWordCount <text path> <result path> <parallelism>");
				return false;
			}
		} else {
			System.out.println("Executing WordCount example with built-in default data.");
			System.out.println("  Provide parameters to read input data from a file.");
			System.out.println("  Usage: YarnWordCount <text path> <result path> <parallelism>");
		}
		return true;
	}
}
