import sbt.librarymanagement.CrossVersion

object Settings {

  // see https://tpolecat.github.io/2017/04/25/scalac-flags.html
  // see https://gist.github.com/tabdulradi/aa7450921756cd22db6d278100b2dac8
  def scalacOptions(scalaVersion: String) = {
    val nProcs = Runtime.getRuntime().availableProcessors().toString
    Seq(
      // format: off
      "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
      "-encoding", "utf-8",                // Specify character encoding used by source files.
      "-explaintypes",                     // Explain type errors in more detail.
      "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
      "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
      "-language:experimental.macros",     // Allow macro definition (besides implementation and application). Disabled, as this will significantly change in Scala 3
      "-language:higherKinds",             // Allow higher-kinded types
      "-language:implicitConversions",     // Allow definition of implicit functions called views. Disabled, as it might be dropped in Scala 3. Instead use extension methods (implemented as implicit class Wrapper(val inner: Foo) extends AnyVal {}
      "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
      "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
      "-Xfatal-warnings",                  // Fail the compilation if there are any warnings.

      "-Xlint:adapted-args",               // Warn if an argument list is modified to match the receiver.
      "-Xlint:constant",                   // Evaluation of a constant arithmetic expression results in an error.
      "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
      "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
      "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
      "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
      "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
      "-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
      "-Xlint:option-implicit",            // Option.apply used implicit view.
      "-Xlint:package-object-classes",     // Class or object defined in package object.
      "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
      "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
      "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
      "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in scope.
      "-Xlint:unused",                     // TODO check if we still need -Wunused below

      "-Ybackend-parallelism", nProcs,                 // Enable paralellisation
      "-Ycache-plugin-class-loader:last-modified",  // Enables caching of classloaders for compiler plugins
      "-Ycache-macro-class-loader:last-modified"    // and macro definitions. This can lead to performance improvements.
      // format: on
    )  ++ (CrossVersion.partialVersion(scalaVersion) match {
      case Some((2, 13)) =>
        Seq(
          // format: off
          "-Xlint:nonlocal-return",            // A return statement used an exception for flow control.
          "-Xlint:implicit-not-found",         // Check @implicitNotFound and @implicitAmbiguous messages.
          "-Xlint:implicit-recursion",         // An implicit resolves to an enclosing definition.
          "-Xlint:serial",                     // @SerialVersionUID on traits and non-serializable classes.
          "-Xlint:valpattern",                 // Enable pattern checks in val definitions.
          "-Xlint:eta-zero",                   // Warn on eta-expansion (rather than auto-application) of zero-ary method.
          "-Xlint:eta-sam",                    // Warn on eta-expansion to meet a Java-defined functional interface that is not explicitly annotated with @FunctionalInterface.
          "-Xlint:deprecation",                // Enable linted deprecations.

          "-Wdead-code",                       // Warn when dead code is identified.
          "-Wextra-implicit",                  // Warn when more than one implicit parameter section is defined.
          "-Wmacros:both",                     // Lints code before and after applying a macro
          "-Wnumeric-widen",                   // Warn when numerics are widened.
          "-Woctal-literal",                   // Warn on obsolete octal syntax.
          "-Wunused:imports",                  // Warn if an import selector is not referenced.
          "-Wunused:patvars",                  // Warn if a variable bound in a pattern is unused.
          "-Wunused:privates",                 // Warn if a private member is unused.
          "-Wunused:locals",                   // Warn if a local definition is unused.
          "-Wunused:explicits",                // Warn if an explicit parameter is unused.
          "-Wunused:implicits",                // Warn if an implicit parameter is unused.
          "-Wunused:params",                   // Enable -Wunused:explicits,implicits.
          "-Wunused:linted",
          // "-Wvalue-discard",                // Warn when non-Unit expression results are unused.
          // format: on
        )
      case Some((2, 12)) =>
        Seq(
          // format: off
          "-Ywarn-dead-code",                  // Warn when dead code is identified.
          "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
          "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
          "-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
          "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
          "-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
          "-Ywarn-numeric-widen",              // Warn when numerics are widened.
          "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
          "-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
          "-Ywarn-unused:locals",              // Warn if a local definition is unused.
          "-Ywarn-unused:params",              // Warn if a value parameter is unused.
          "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
          "-Ywarn-unused:privates",            // Warn if a private member is unused.
          // "-Ywarn-value-discard"            // Warn when non-Unit expression results are unused.
          // format: on
        )
      case _ =>
        throw new IllegalArgumentException(s"Unsupported scala version $scalaVersion")
    })
  }
}
