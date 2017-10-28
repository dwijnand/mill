package forge

import java.nio.{file => jnio}

import forge.Target.test
import utest._

object EvaluationTests extends TestSuite{

  val tests = Tests{
    val baseCtx = DefCtx("forge.ForgeTests.tests ", None)

    val (singleton, pair, anonTriple, diamond, anonDiamond, bigSingleTerminal) = TestUtil.makeGraphs()
    'evaluateSingle - {
      val evaluator = new Evaluator(jnio.Paths.get("target/workspace"), baseCtx)

      def check(target: Target[_], expValue: Any, expEvaled: OSet[Target[_]]) = {
        val Evaluator.Results(returnedValues, returnedEvaluated) = evaluator.evaluate(OSet(target))
        assert(
          returnedValues == Seq(expValue),
          returnedEvaluated == expEvaled
        )
        // Second time the value is already cached, so no evaluation needed
        val Evaluator.Results(returnedValues2, returnedEvaluated2) = evaluator.evaluate(OSet(target))
        assert(
          returnedValues2 == returnedValues,
          returnedEvaluated2 == OSet()
        )
      }

      'singleton - {
        import singleton._
        // First time the target is evaluated
        check(single, expValue = 0, expEvaled = OSet(single))

        single.counter += 1
        // After incrementing the counter, it forces re-evaluation
        check(single, expValue = 1, expEvaled = OSet(single))
      }
      'pair - {
        import pair._
        check(down, expValue = 0, expEvaled = OSet(up, down))

        down.counter += 1
        check(down, expValue = 1, expEvaled = OSet(down))

        up.counter += 1
        check(down, expValue = 2, expEvaled = OSet(up, down))
      }
      'anonTriple - {
        import anonTriple._
        val middle = down.inputs(0)
        check(down, expValue = 0, expEvaled = OSet(up, middle, down))

        down.counter += 1
        check(down, expValue = 1, expEvaled = OSet(middle, down))

        up.counter += 1
        check(down, expValue = 2, expEvaled = OSet(up, middle, down))

        middle.asInstanceOf[Target.Test].counter += 1

        check(down, expValue = 3, expEvaled = OSet(middle, down))
      }
      'diamond - {
        import diamond._
        check(down, expValue = 0, expEvaled = OSet(up, left, right, down))

        down.counter += 1
        check(down, expValue = 1, expEvaled = OSet(down))

        up.counter += 1
        // Increment by 2 because up is referenced twice: once by left once by right
        check(down, expValue = 3, expEvaled = OSet(up, left, right, down))

        left.counter += 1
        check(down, expValue = 4, expEvaled = OSet(left, down))

        right.counter += 1
        check(down, expValue = 5, expEvaled = OSet(right, down))
      }
      'anonDiamond - {
        import anonDiamond._
        val left = down.inputs(0).asInstanceOf[Target.Test]
        val right = down.inputs(1).asInstanceOf[Target.Test]
        check(down, expValue = 0, expEvaled = OSet(up, left, right, down))

        down.counter += 1
        check(down, expValue = 1, expEvaled = OSet(left, right, down))

        up.counter += 1
        // Increment by 2 because up is referenced twice: once by left once by right
        check(down, expValue = 3, expEvaled = OSet(up, left, right, down))

        left.counter += 1
        check(down, expValue = 4, expEvaled = OSet(left, right, down))

        right.counter += 1
        check(down, expValue = 5, expEvaled = OSet(left, right, down))
      }
//      'anonImpureDiamond - {
//        import AnonImpureDiamond._
//        val left = down.inputs(0).asInstanceOf[Target.Test]
//        val right = down.inputs(1).asInstanceOf[Target.Test]
//        check(down, expValue = 0, expEvaled = Seq(up, left, right, down))
//
//        down.counter += 1
//        check(down, expValue = 1, expEvaled = Seq(left, down))
//      }
    }


    'full - {

    }
  }
}
