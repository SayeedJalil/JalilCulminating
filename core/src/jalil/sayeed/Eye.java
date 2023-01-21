package jalil.sayeed;

import com.badlogic.gdx.ai.steer.Steerable;
import com.badlogic.gdx.ai.utils.Location;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;

import static jalil.sayeed.Utils.Constants.PPM;

public class Eye{
    private int health;
    private boolean canBeHit;
    private boolean isAttacked;
    private boolean dead;
    private enum State {DEAD, FLYING, ATTACKING, HURT}
    private boolean lookRight;
    public Animation<TextureRegion> fly;
    TextureRegion currentFrame;
    private World world;
    private State currentState;
    private State previousState;
    private Animation<TextureRegion> move;
    private Animation<TextureRegion> attack;
    private Animation<TextureRegion> hurt;
    private Animation<TextureRegion> death;
    private float stateTimer;
    private boolean runningRight;
    private boolean isAttacking;
    private Body body;
    private int x;
    private int y;
    private SpriteBatch batch;
    private float timer;
    private float timer2;
    public boolean onFloor;
    private float attackTimer;
    private boolean isHitting;

    /**
     * Constructor for Eye class
     * @param x
     * @param y
     * @param world
     * @param death
     * @param fly
     * @param hurt
     * @param attack
     * @param batch
     */
    Eye(int x, int y, World world, Animation<TextureRegion> death, Animation<TextureRegion> fly, Animation<TextureRegion> hurt, Animation<TextureRegion> attack, SpriteBatch batch) {
        this.x = x;
        this.y = y;
        this.world = world;
        this.death = death;
        this.hurt = hurt;
        this.attack = attack;
        this.batch = batch;
        this.fly = fly;
        isHitting = false;
        dead = false;
        isAttacked = false;
        canBeHit = true;
        health = 4;
        isAttacking = false;
        stateTimer = 0;
        timer = 0;
        lookRight = true;
        timer2 = 0;
        attackTimer = 0;
        onFloor = false;
    }

    /**
     * Updates the eye
     * @param delta
     * @param isPlayerAttacking
     * @param playerBody
     * @param attackTime
     */
    public void update(float delta, boolean isPlayerAttacking, Body playerBody, float attackTime) {
        timer += delta;

        int velocityX = 0;
        int velocityY = 0;

        // Runs basic movement for the eye that follows the player if it isn't dead
        if(!dead) {
            if (playerBody.getPosition().x > body.getPosition().x) {
                velocityX += 1;
                lookRight = true;
            }
            if (playerBody.getPosition().x < body.getPosition().x) {
                velocityX -= 1;
                lookRight = false;
            }
            if (playerBody.getPosition().y < body.getPosition().y) {
                velocityY -= 1;
            }
            if (playerBody.getPosition().y > body.getPosition().y) {
                velocityY += 1;
            }
        }

        // Stops the Eye if its attacked and moves the eye if isAttacked is false
        if(isAttacked){
            body.setLinearVelocity(0, 0);
        } else {
            body.setLinearVelocity(velocityX * 2, velocityY * 2);
        }
        draw(delta);
        hit(isPlayerAttacking, playerBody, attackTime);
    }

    /**
     * Draw the Eye's current frame
     * @param delta
     */
    public void draw(float delta) {
        getFrame(delta);
        batch.begin();
        batch.draw(currentFrame, body.getPosition().x * PPM - 80, body.getPosition().y * PPM - 70, currentFrame.getRegionWidth(), currentFrame.getRegionHeight());
        batch.end();
    }

    /**
     * Gets the currentFrame of the Eye depending on it's curret state
     * @param dt
     */
    public void getFrame(float dt) {
        currentState = getState();

        switch (currentState) {
            case DEAD:
                currentFrame = death.getKeyFrame(stateTimer, false);
                break;
            case HURT:
                currentFrame = hurt.getKeyFrame(stateTimer, false);
                break;
            case ATTACKING:
                attackTimer += dt;
                currentFrame = attack.getKeyFrame(stateTimer, true);
                break;
            case FLYING:
            default:
                currentFrame = fly.getKeyFrame(stateTimer, true);
        }

        // Flips the Eye depending on which side its facing
        if (!lookRight && !currentFrame.isFlipX()) {
            currentFrame.flip(true, false);
        } else if (lookRight && currentFrame.isFlipX()) {
            currentFrame.flip(true, false);
        }
        stateTimer = currentState == previousState ? stateTimer + dt : 0;
        previousState = currentState;
    }

    /**
     * Creates the Eye's body
     * @param x
     * @param y
     */
    public void createBody(int x, int y) {
        // Initialize body
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.KinematicBody;
        bodyDef.position.set(x, y);
        bodyDef.fixedRotation = false;

        body = world.createBody(bodyDef);

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(32 / 2.2f / PPM, 32 / 2 / PPM / 1.2f);

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = shape;
        fixtureDef.density = 0;
        fixtureDef.friction = 0.8f;
        fixtureDef.restitution = 0;
        fixtureDef.filter.categoryBits = (short) 2;
        fixtureDef.filter.maskBits = (short) 2;


        body.createFixture(fixtureDef).setUserData(this);

        shape.dispose();
    }

    /**
     * Gets the Eye's body
     * @return
     */
    public Body getBody() {
        createBody(x, y);
        return body;
    }

    /**
     * Gets the Eyes current state based on what it is doing
     * @return
     */
    public State getState() {
        if (dead) {
            return State.DEAD;
        } else if (isAttacked) {
            return State.HURT;
        } else if (isAttacking) {
            return State.ATTACKING;
        } else {
            return State.FLYING;
        }
    }

    /**
     * Checks if the player is hit
     * @param isPlayerAttacking
     * @param playerBody
     * @param attackTime
     */
    public void hit(boolean isPlayerAttacking, Body playerBody, float attackTime){
        // Creates an area where the player can hit the Eye if the player is attacking and detects if it is attacked or not
        if(playerBody.getPosition().x - body.getPosition().x < 2 && playerBody.getPosition().x - body.getPosition().x > -1 && isPlayerAttacking && attackTime > 0.2 && canBeHit){
            isAttacked = true;
            health -= 1;

        } else if(body.getPosition().x - playerBody.getPosition().x < 2 && body.getPosition().x - playerBody.getPosition().x > -1 && isPlayerAttacking && attackTime > 0.2 && canBeHit){
            isAttacked = true;
            health -= 1;
        }
        // Resets variables
        if(attackTime > 0 && isAttacked){
            canBeHit = false;
        } else{
            canBeHit = true;
            isAttacked = false;
        }
        if(health <= 0){
            dead = true;
        }
    }

    public void attack(Body playerBody, float delta){
        if(!(attackTimer > 0)){
            isAttacking = false;
            isHitting = false;
        }

        if(playerBody.getPosition().x - body.getPosition().x < 4 && playerBody.getPosition().x - body.getPosition().x > -1 && playerBody.getPosition().y < body.getPosition().y + 1.4 && playerBody.getPosition().y > body.getPosition().y - 1){

            isAttacking = true;
            if(attackTimer > 1.5 && playerBody.getPosition().x - body.getPosition().x < 4 && playerBody.getPosition().x - body.getPosition().x > -1){
                isHitting = true;
            }
        } else if(body.getPosition().x - playerBody.getPosition().x < 4 && body.getPosition().x - playerBody.getPosition().x > -1 && playerBody.getPosition().y < body.getPosition().y + 1.4 && playerBody.getPosition().y > body.getPosition().y - 1){
            isAttacking = true;
            if(attackTimer > 1.5 && body.getPosition().x - playerBody.getPosition().x < 4 && body.getPosition().x - playerBody.getPosition().x > -1){
                isHitting = true;
            }
        }
        if(dead){
            isAttacking = false;
            isHitting = false;
        }

        if(attackTimer > 2.5){
            attackTimer = 0;
            isAttacking = false;
            isHitting = false;
        }

    }
}
