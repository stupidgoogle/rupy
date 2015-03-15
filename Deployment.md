The classloader in rupy is very simplistic, so you have to be careful how you write your hot-deployable code and what you store in the session. We recommend that you don't store hot-deployed classes with the `Session.put(key, value)` method, since `Session.get(key)` will throw a `ClassCastException` if you hot-deploy your application.

**Think Small**

We store a _String_ value, for example: `Session.put("key", "XT4E")` that maps to the actual data object in a hashmap. Because _String_ is a bootclasspath class, old sessions can keep running the old code while new sessions run the new code without classloading issues. The user experience of a hot-deployment becomes seamless.

Another positive side-effect of limitations in Rupy is the lack of hot-deployed libs; since we only hot-deploy the application code, and libs are added to the bootclasspath, your server runs out of memory later rather than sooner during development. This is also true for the live environment of course.

**Common Solutions** (in order of popularity)

  * Make sure the hot-deployed classes are not in the bootclasspath.
  * Don't try to load a hot-deployed class with `Class.forName(String)`.
  * All classes have to have a void attribute constructor.
  * Inner classes need to be public static.
  * Implemented interfaces in the bootclasspath need to be public, even if the hot-deployed implementing class is in the same package.
  * Don't instantiate the class statically inside itself.

```
    public class Lock {
        static Lock lock = new Lock();
    }
```

  * Deploy static resource bundles az **.zip** instead of **.jar** to avoid them being copied at server restart.