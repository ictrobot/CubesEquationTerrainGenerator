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

@Mod
public class EquationTerrainGenerator {
  private static Block block;
  
  @ModEventHandler
  public void preInit(PreInitializationEvent event) {
    block = IDManager.toBlock("equationterraingenerator:block");
    GeneratorManager.register("equationterraingenerator:equation", new TerrainGeneratorFactory() {
      @Override
      public TerrainGenerator getTerrainGenerator(SaveOptions saveOptions) {
        return new Generator(saveOptions.worldSeedString, false);
      }
    });
    GeneratorManager.register("equationterraingenerator:equationfilled", new TerrainGeneratorFactory() {
      @Override
      public TerrainGenerator getTerrainGenerator(SaveOptions saveOptions) {
        return new Generator(saveOptions.worldSeedString, true);
      }
    });
  }
  
  public static class Generator extends TerrainGenerator {
    private final String eqn;
    private final boolean filled;
    private ThreadLocal<Expression> expression = new ThreadLocal<Expression>() {
      @Override
      protected Expression initialValue() {
        return getExpression();
      }
    };
    
    public Generator(String eqn, boolean filled) {
      this.eqn = eqn;
      this.filled = filled;
      Log.info("Equation " + (filled ? "Filled " : "") + "Terrain Generator: " + eqn);
      try {
        ValidationResult validationResult = getExpression().validate(false);
        if (!validationResult.isValid()) throw new RuntimeException(String.valueOf(validationResult.getErrors()));
      } catch (Exception e) {
        Debug.crash(e);
      }
    }
  
    @Override
    public void generate(Area area) {
      for (int x = 0; x < Area.SIZE_BLOCKS; x++) {
        for (int z = 0; z < Area.SIZE_BLOCKS; z++) {
          int y = height(x + area.minBlockX, z + area.minBlockZ);
          if (filled) {
            for (int i = y; i >= 0; i--) {
              block(area, x, i, z);
            }
          } else {
            if (y != -1) block(area, x, y, z);
          }
        }
      }
    }
    
    private void block(Area area, int x, int y, int z) {
      int meta = (y / 5) % 41;
      if (meta > 20) meta = 40 - meta;
      setVisible(area, block, x, y, z, meta);
    }
  
    @Override
    public void features(Area area, WorldServer world) {
    
    }
  
    @Override
    public BlockReference spawnPoint(WorldServer world) {
      int y = height(0, 0);
      if (y == -1) return new BlockReference().setFromBlockCoordinates(0, 100, 0);
      return new BlockReference().setFromBlockCoordinates(0, y + 2, 0);
    }
    
    private Expression getExpression() {
      return new ExpressionBuilder(eqn).variable("x").variable("z").build();
    }
    
    private int height(int x, int z) {
      try {
        Expression e = expression.get();
        e.setVariable("x", x);
        e.setVariable("z", z);
        int evaluate = (int) e.evaluate();
        return evaluate < 0 ? -1 : evaluate;
      } catch (Exception e) {
        return -1;
      }
    }
  }
}
