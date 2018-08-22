{
    var STRIP_COMMENTS = /((\/\/.*$)|(\/\*[\s\S]*?\*\/))/mg;
    var ARGUMENT_NAMES = /([^\s,]+)/g;
    function getParamNames(func) {
      var a = func.toString().replace(STRIP_COMMENTS, '');
      var r = a.slice(a.indexOf('(')+1, a.indexOf(')')).match(ARGUMENT_NAMES);
      var l = new java.util.ArrayList();
      if(r !== null) {
          for (var i = 0; i < r.length; i++) {
              l.add(r[i]);
          }
      }
      return l;
    }

    function getAllFunctions(){
      var a = new java.util.ArrayList();
      for (var f in this){
        if (this.hasOwnProperty(f) && this[f] instanceof Function && !/a/i.test(f)){
          a.add(this[f]);
        }
      }
      return a;
    }

    var functions = getAllFunctions();
    var commands = new java.util.ArrayList()
    for (var i = 0; i < functions.length; i++) {
        var f = functions[i];
        if (f.hasOwnProperty('desc'))
        {
            if (!f.hasOwnProperty('permission')) f.permission = "fawe.use";
            if (!f.hasOwnProperty('aliases')) f.aliases = [f.name];
            var cmd = com.boydti.fawe.config.Commands.fromArgs(f.aliases, f.usage, f.desc, f.min, f.max, f.flags, f.help);
            var man = com.sk89q.worldedit.extension.platform.CommandManager.getInstance();
            var builder = man.getBuilder();
            var args = getParamNames(f);

            var wrap = Java.extend(java.util.function.Function, {
                apply: function(a) {
                    return f.apply(null, a);
                }
            });
            var w2 = new wrap();
            var callable = new com.sk89q.worldedit.util.command.parametric.FunctionParametricCallable(builder, "", cmd, f.permission, args, w2);
            commands.add(callable);
        }
    }
    commands;
}