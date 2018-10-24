# project-insights

## Endpoints provided

- `http://<domain>/` -> provides available variable names
- `http://<domain>/dependencies?variable=<variable_name>`  -> mutual information between the given variable and 
the rest of the variables listed in descending order as a csv file 

## Using sbt in local

**1.** To compile the app using sbt `sbt compile`

**2.** To run the app on the development server using sbt `sbt run`

**3.** To run the tests using sbt `sbt test`

## Using heroku in local

**1.** To compile the app `sbt compile stage`

**2.** To run the app `heroku local`

**Note:** Do not forget to add app secret to key application.conf file using command `playGenerateSecret` in Play shell.

## Deploying to heroku

Refer to: `https://devcenter.heroku.com/articles/getting-started-with-scala`

**Note:** Do not forget to add allowed hosts to application.conf
