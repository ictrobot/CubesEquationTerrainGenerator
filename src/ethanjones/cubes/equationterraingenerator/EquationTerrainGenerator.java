package ethanjones.cubes.equationterraingenerator;

import ethanjones.cubes.block.Block;
import ethanjones.cubes.core.id.IDManager;
import ethanjones.cubes.core.logging.Log;
import ethanjones.cubes.core.mod.Mod;
import ethanjones.cubes.core.mod.ModEventHandler;
import ethanjones.cubes.core.mod.event.PreInitializationEvent;
import ethanjones.cubes.core.system.Debug;
import ethanjones.cubes.world.generator.GeneratorManager;
import ethanjones.cubes.world.generator.GeneratorManager.TerrainGeneratorFactory;
import ethanjones.cubes.world.generator.TerrainGenerator;
import ethanjones.cubes.world.reference.BlockReference;
import ethanjones.cubes.world.save.SaveOptions;
import ethanjones.cubes.world.server.WorldServer;
import ethanjones.cubes.world.storage.Area;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import net.objecthunter.exp4j.ValidationResult;

import java.util.regex.Pattern;

@Mod
public class EquationTerrainGenerator {
  private static Block block;
  
  @ModEventHandler
  public void preInit(PreInitializationEvent event) {
    block = IDManager.toBlock("equationterraingenerator:block");
    GeneratorManager.register("equationterraingenerator:equation", new TerrainGeneratorFactory() {
      @Override
      public TerrainGenerator getTerrainGenerator(SaveOptions saveOptions) {
        return new NormalGenerator(saveOptions.worldSeedString);
      }
    });
    GeneratorManager.register("equationterraingenerator:equationfilled", new TerrainGeneratorFactory() {
      @Override
      public TerrainGenerator getTerrainGenerator(SaveOptions saveOptions) {
        return new FilledGenerator(saveOptions.worldSeedString);
      }
    });
    GeneratorManager.register("equationterraingenerator:equationdifference", new TerrainGeneratorFactory() {
      @Override
      public TerrainGenerator getTerrainGenerator(SaveOptions saveOptions) {
        return new DifferenceGenerator(saveOptions.worldSeedString);
      }
    });
  }
  
  public static abstract class Generator extends TerrainGenerator {
    protected final String[] eqn;
    protected ThreadLocal<Expression[]> expressionThreadLocal = new ThreadLocal<Expression[]>() {
      @Override
      protected Expression[] initialValue() {
        Expression[] expressions = new Expression[eqn.length];
        for (int i = 0; i < eqn.length; i++) {
          expressions[i] = getExpression(eqn[i]);
        }
        return expressions;
      }
    };
    
    public Generator(String eqn) {
      Log.info("Equation Terrain " + getClass().getSimpleName() + ": " + eqn);
      this.eqn = eqn.split(Pattern.quote("|"));
      try {
        for (Expression expression : expressionThreadLocal.get()) {
          ValidationResult validationResult = expression.validate(false);
          if (!validationResult.isValid()) throw new RuntimeException("Invalid expression " + String.valueOf(validationResult.getErrors()));
        }
      } catch (Exception e) {
        Debug.crash(new RuntimeException("Invalid expression", e));
      }
    }
    
    protected void block(Area area, int x, int y, int z) {
      int meta = (y / 5) % 41;
      if (meta > 20) meta = 40 - meta;
      set(area, block, x, y, z, meta);
    }
    
    @Override
    public void features(Area area, WorldServer world) {
      
    }
    
    @Override
    public BlockReference spawnPoint(WorldServer world) {
      int y = -1;
      for (Expression expression : expressionThreadLocal.get()) {
        int i = height(expression, 0, 0);
        if (i > y) y = i;
      }
      if (y == -1) return new BlockReference().setFromBlockCoordinates(0, 100, 0);
      return new BlockReference().setFromBlockCoordinates(0, y + 2, 0);
    }
    
    protected Expression getExpression(String eqn) {
      return new ExpressionBuilder(eqn).variable("x").variable("z").build();
    }
    
    protected int height(Expression e, int x, int z) {
      try {
        e.setVariable("x", x);
        e.setVariable("z", z);
        int evaluate = (int) e.evaluate();
        return evaluate < 0 ? -1 : evaluate;
      } catch (Exception exception) {
        return -1;
      }
    }
  }
  
  public static class NormalGenerator extends Generator {
    
    public NormalGenerator(String eqn) {
      super(eqn);
    }
    
    @Override
    public void generate(Area area) {
      Expression[] expressions = expressionThreadLocal.get();
      for (int x = 0; x < Area.SIZE_BLOCKS; x++) {
        for (int z = 0; z < Area.SIZE_BLOCKS; z++) {
          for (Expression e : expressions) {
            int y = height(e, x + area.minBlockX, z + area.minBlockZ);
            if (y != -1) block(area, x, y, z);
          }
        }
      }
    }
    
  }
  
  public static class FilledGenerator extends Generator {
    
    public FilledGenerator(String eqn) {
      super(eqn);
    }
    
    @Override
    public void generate(Area area) {
      Expression[] expressions = expressionThreadLocal.get();
      for (int x = 0; x < Area.SIZE_BLOCKS; x++) {
        for (int z = 0; z < Area.SIZE_BLOCKS; z++) {
          int minHeight = 0;
          for (Expression e : expressions) {
            int y = height(e, x + area.minBlockX, z + area.minBlockZ);
            for (int i = y; i >= minHeight; i--) {
              block(area, x, i, z);
            }
            if (y > minHeight) y = minHeight + 1;
          }
        }
      }
    }
    
  }
  
  public static class DifferenceGenerator extends Generator {
    
    public DifferenceGenerator(String eqn) {
      super(eqn);
    }
    
    @Override
    public void generate(Area area) {
      Expression[] expressions = expressionThreadLocal.get();
      for (int x = 0; x < Area.SIZE_BLOCKS; x++) {
        for (int z = 0; z < Area.SIZE_BLOCKS; z++) {
          int minHeight = -1, maxHeight = -1;
          for (Expression e : expressions) {
            int y = height(e, x + area.minBlockX, z + area.minBlockZ);
            if (y != -1 && (minHeight == -1 || y < minHeight)) minHeight = y;
            if (y > maxHeight) maxHeight = y;
          }
          if (minHeight != -1) {
            for (int y = minHeight; y <= maxHeight; y++) {
              block(area, x, y, z);
            }
          }
        }
      }
    }
    
  }
}
