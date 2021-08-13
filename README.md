# nbaStats

We are interested in the NBA and particularly how different awards are given out. As a result,
designed a program that takes into the account the statistics of each player in the league and calculates
a score using the data. We begin with a table that displays each player and their statistics. From there, 
depending on the award chosen (MVP, ROTY, DPOY), uses different statistics to narrow down candidates for
each award and show which players have the best statistics for the award. Furthermore, the weight that 
each statistic carries in the calculations can be modified to best fit the user's preference for which 
statistics that matter most. We also have a MVP-BFS function that finds a path of players, beginning with
an MVP and through their teammates, creating a path to another MVP. All in all, we have designed a
program that compiles the statistics of all current NBA players in a table. From there, we can narrow down
the table based on whichever award is chosen by completing calculation with their statistics to obtain an
award score, which is automatically sorted to list the best players fit for that award. Finally, we have 
also included an additional function that finds the shortest path between one MVP to another MVP using 
teammates. 

Here is a manual that we have created so that you can see how our code works:
[NBA Award Predictor_ PROJECT MANUAL.pdf](https://github.com/harrisonly2/nbaStats/files/6982470/NBA.Award.Predictor_.PROJECT.MANUAL.pdf)
