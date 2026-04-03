# Documentation deploy process for Tarantool Java SDK

The Tarantool Java SDK documentation is located on the GitHub pages. The link can be found on the
main page of the repository.

The documentation is deploying under two different scenarios:

1. `dev` - unreleased documentation. These documents contain all the information about capabilities
   and features of the Tarantool Java SDK that have not yet been released (tag). This includes
   information about any innovations and opportunities currently under development. **Information is
   subject to change and may not be accurate until next release.**
2. `X.Y.*` is the release version of the documentation. Here is the released information about
   capabilities and features of the Tarantool Java SDK that are specific to a particular release.
   Not here contains information about innovations that go beyond the scope of this release.
   Information in documentation **can be supplemented, clarified and revised only about those
   recorded in this release capabilities**. Also, information **can be corrected if errors are
   found in the text documentation**.

## Documentation deploy process

### Dev documentation

`dev-documentation` is deployed every time changes from branches are pushed to master
`master` branch.

### Release documentation

Release documentation is deployed manually. In order to deploy documentation for a specific
*major-minor** version of the Tarantool Java SDK, follow these steps:

* Go to the `Actions` section and select `release-documentation` workflow
* Click **Run workflow** and select the required tag (`Run workflow` → `Tags`)
* **(Only for tags _1.5.x_ and _1.6.x_)** Select tag `1.6.0` and enter the required tag, e.x. 
  `1.5.101` (this is necessary because these tags work on the old compatible workflow)
