# Cubes Equation Terrain Generator

When using this mod, select "Equation" from the terrain generator drop down, and enter an equation using x and/or z into the seed textbox.

[exp4j](http://www.objecthunter.net/exp4j/) is used to evaluate the equation.

## Terrain Generators
The vertical bar ( | ) symbol can be used to enter multiple equations. The terrain is generated using a block which changes colour every 5 blocks to show height difference clearly. 

**Equation** generator will evaluate each equation and place a block at the y value returned

**Equation Filled** generator will evaluate each equation and fill from the highest y valued returned to 0

**Equation Difference** generator will evaluate each equation and fill from the lowest returned y value to the highest returned value

## Examples
 - Sphere: ```50-sqrt(-x^2-z^2+2500) | 50+sqrt(-x^2-z^2+2500)``` (best on Equation Difference)
 - Ripples ```20sin(sqrt(x^2 + z^2) * 0.05) + 40```
 - Hills: ```25sin(x * 0.02) + 25sin(z * 0.02) + 50```

## Images
![Image 1](/images/readme1.png)
![Image 2](/images/readme2.png)
![Image 3](/images/readme3.png)