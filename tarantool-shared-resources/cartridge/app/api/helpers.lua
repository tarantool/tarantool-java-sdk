local function init_module()
    rawset(_G, "echo", function(...)
        return ...
    end)
    rawset(_G, "get_static", function()
        return 1, "hi", false
    end)
end

return {
    init_module = init_module
}
