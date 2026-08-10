return {
  -- Args must be the table!
  get_by_id = function(args)
    local repository = require("repository")
    return repository.get(args.model_type, args.id);
  end,
  say_hello = function()
    return 'hello!';
  end
}
